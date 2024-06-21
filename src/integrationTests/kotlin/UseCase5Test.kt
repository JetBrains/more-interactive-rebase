package com.jetbrains.interactiveRebase.integrationTests

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.testFramework.TestActionEvent.createTestEvent
import com.jetbrains.interactiveRebase.actions.buttonActions.StartRebaseAction
import com.jetbrains.interactiveRebase.actions.changePanel.AddBranchAction
import com.jetbrains.interactiveRebase.actions.changePanel.RedoAction
import com.jetbrains.interactiveRebase.actions.changePanel.UndoAction
import com.jetbrains.interactiveRebase.actions.gitPanel.FixupAction
import com.jetbrains.interactiveRebase.actions.gitPanel.RewordAction
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.checkout
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.git
import com.jetbrains.interactiveRebase.listeners.BranchNavigationListener
import com.jetbrains.interactiveRebase.listeners.TextFieldListener
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.ModelService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.awaitility.Awaitility
import java.awt.event.MouseEvent
import java.util.concurrent.TimeUnit

/**
 * Tests a use case in which the following actions are performed:
 * 1. Open the plugin and initializes it with the feature branch
 * 2. Reword a commit ("i love testing" into "I swear this is reworded:)")
 * 3. Reorder a commit ("my final commit" to be the second-to-last commit)
 * 4. Reorder a commit ("code quality" to be the last commit)
 * 5. Click the rebase button to do the actions
 * 6. Checkout to the main branch and create 2 new commits
 * 7. Go back to the development branch
 * 8. Add the main branch to the view
 * 9. Change the base to be the second commit on the main branch
 * 10. Do 2 reorders
 * 11. Do a fixup
 * 12. Undo the fixup
 * 13. Undo one reorder
 * 14. Redo reorder
 * 15. Redo the fixup
 * 16. Rebase again
 */
class UseCase5Test : IRGitPlatformTest() {
    fun testUseCase5() {
        runBlocking(Dispatchers.EDT) {
            // this opens the editor tab, and initializes everything
            openAndInitializePlugin()
            val modelService = project.service<ModelService>()
            val nav = BranchNavigationListener(project, modelService)
            // this selects the second-to-last commit ("i love testing")
            nav.down()
            nav.down()

            // this "sets up" the commit to be reworded, by enabling the text field
            val rewordAction = RewordAction()
            val testEvent1 = createTestEvent(rewordAction)
            rewordAction.update(testEvent1)
            assertThat(testEvent1.presentation.isEnabled).isTrue()
            rewordAction.actionPerformed(testEvent1)

            // here we pretend that we are a user inputting the data new commit message
            // in the GUI, by getting the listener and setting the text field to the new message
            val labeledBranchPanel = project.service<ActionService>().mainPanel.graphPanel.mainBranchPanel
            val textField = labeledBranchPanel.getTextField(1)

            val listener = textField.keyListeners[0] as TextFieldListener
            assertThat(listener).isNotNull()

            listener.textField.text = "I swear this is reworded:)"

            // here we pressed enter after typing the new message
            listener.processEnter()

            // this reorders the last commit to be second-to-last
            nav.down()
            nav.altDown()

            // this reorders "code quality" to be the last commit
            nav.down()
            nav.down()
            nav.altUp()

            // here we click the rebase button
            val rebaseAction = StartRebaseAction()
            val rebaseEvent = createTestEvent(rebaseAction)
            rebaseAction.actionPerformed(rebaseEvent)

            Awaitility.await()
                .atMost(20000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .alias("message is reworded")
                .pollInSameThread()
                .until {
                    val commitMessage = repository.git("log --format=%B -n 1 HEAD")
                    commitMessage.equals("I swear this is reworded:)\n")
                }

            assertThat(gitCommitsCountEquals(4)).isTrue()
            var remainingCommitMessages = repository.getAllCommitMessages()
            remainingCommitMessages = remainingCommitMessages.filter { it != "initial" }
            assertThat(remainingCommitMessages).isEqualTo(
                listOf(
                    "I swear this is reworded:)",
                    "my final commit",
                    "first",
                    "code quality",
                ),
            )

            // checkout on main and create 2 new commits
            repository.checkout("main")
            assertCorrectCheckedOutBranch("main")
            createAndCommitNewFile("file5.txt", "second")
            createAndCommitNewFile("file6.txt", "third")

            Awaitility.await()
                .pollInSameThread()
                .alias("2 new commits on main")
                .atMost(15000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until {
                    gitCommitsCountEquals(2)
                }

            // go back to development branch and rebase
            repository.checkout("development")
            assertCorrectCheckedOutBranch(developmentBranch)

            Awaitility.await()
                .atMost(15000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until { modelService.branchInfo.initialCommits.size == 4 }
            assertThat(modelService.branchInfo.name).isEqualTo(developmentBranch)

            // this adds the main branch to the view
            val addBranchAction = AddBranchAction()
            val testEvent2 = createTestEvent(addBranchAction)
            addBranchAction.actionPerformed(testEvent2)

            val sidePanel =
                project.service<ActionService>()
                    .mainPanel.sidePanel
            val sidePanelPane = project.service<ActionService>().mainPanel.sidePanelPane
            assertThat(sidePanelPane.isVisible).isTrue()

            Awaitility.await()
                .pollInSameThread()
                .alias("open side panel and check if branches are displayed")
                .atMost(30000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until {
                    sidePanel.branches.equals(listOf("main"))
                }

            val mainBranchPanel = sidePanel.sideBranchPanels[0]
            assertThat(mainBranchPanel.branchName).isEqualTo("main")
            val mainBranchPanelListener = mainBranchPanel.mouseListeners[0]
            val mouseEvent = MouseEvent(mainBranchPanel, 444, 0L, 0, 2, 2, 1, false)

            mainBranchPanelListener.mouseClicked(mouseEvent)

            Awaitility.await()
                .atMost(15000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .alias("adding a second branch to the view and refreshing")
                .pollInSameThread()
                .until {
                    modelService.graphInfo.addedBranch != null
                }

            val addedBranch = modelService.graphInfo.addedBranch
            Awaitility.await()
                .pollInSameThread()
                .alias("add main to view")
                .atMost(15000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until {
                    addedBranch?.name == "main"
                }

            // this selects the second commit on the main branch to be the new base
            nav.down()
            nav.right()
            nav.up()
            val commitOnSecondBranch = modelService.graphInfo.addedBranch?.selectedCommits?.get(0)
            modelService.graphInfo.addedBranch?.baseCommit = commitOnSecondBranch
            project.service<ActionService>().takeNormalRebaseAction()

            // reorders the last commit to be the first one
            nav.left()
            nav.altUp()
            nav.altUp()
            nav.altUp()

            // reorders the second commit to be the third one
            nav.down()
            nav.altDown()

            val fixupAction1 = FixupAction()
            val testEvent3 = createTestEvent(fixupAction1)

            fixupAction1.update(testEvent3)
            assertThat(testEvent3.presentation.isEnabled).isTrue()

            fixupAction1.actionPerformed(testEvent3)
            assertThat(modelService.branchInfo.currentCommits.size).isEqualTo(3)

            val undoAction = UndoAction()
            val undoEvent = createTestEvent(undoAction)
            undoAction.update(undoEvent)
            assertThat(undoEvent.presentation.isEnabled).isTrue()

            undoAction.actionPerformed(undoEvent)
            assertThat(modelService.branchInfo.currentCommits.size).isEqualTo(4)

            val undoEvent2 = createTestEvent(undoAction)
            undoAction.actionPerformed(undoEvent2)

            val redoAction = RedoAction()
            val redoEvent = createTestEvent(redoAction)
            redoAction.update(redoEvent)
            assertThat(redoEvent.presentation.isEnabled).isTrue()

            redoAction.actionPerformed(redoEvent)

            val redoEvent2 = createTestEvent(redoAction)
            redoAction.actionPerformed(redoEvent2)
            assertThat(modelService.branchInfo.currentCommits.size).isEqualTo(3)

            val rebaseEvent2 = createTestEvent(rebaseAction)
            rebaseAction.actionPerformed(rebaseEvent2)

            // asserts that the rebase action was done, moving it further away from the initial commit,
            Awaitility.await()
                .alias("rebase action being done")
                .pollInSameThread()
                .atMost(10000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until {
                    countCommitsSinceSpecificCommit(initialCommitOnMain) == 4
                }

            remainingCommitMessages = repository.getAllCommitMessages()
            remainingCommitMessages = remainingCommitMessages.filter { it != "initial" }
            remainingCommitMessages = remainingCommitMessages.filter { it != "second" }
            assertThat(remainingCommitMessages).isEqualTo(listOf("code quality", "my final commit", "first"))
        }
    }
}
