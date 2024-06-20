package com.jetbrains.interactiveRebase.integrationTests

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.testFramework.TestActionEvent.createTestEvent
import com.jetbrains.interactiveRebase.actions.CreateEditorTabAction
import com.jetbrains.interactiveRebase.actions.buttonActions.ResetAction
import com.jetbrains.interactiveRebase.actions.buttonActions.StartRebaseAction
import com.jetbrains.interactiveRebase.actions.changePanel.AddBranchAction
import com.jetbrains.interactiveRebase.actions.changePanel.RedoAction
import com.jetbrains.interactiveRebase.actions.changePanel.UndoAction
import com.jetbrains.interactiveRebase.actions.gitPanel.RebaseAction
import com.jetbrains.interactiveRebase.dataClasses.commands.RebaseCommand
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.checkout
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.checkoutNew
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.git
import com.jetbrains.interactiveRebase.listeners.BranchNavigationListener
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.awaitility.Awaitility
import java.awt.event.MouseEvent
import java.util.concurrent.TimeUnit

/**
 * This test checks if the plugin can handle two branches being in the view at the same time.
 * It performs the following actions:
 * 1. Opens the plugin and initializes it with the feature branch
 * 2. Adds the main branch to the view
 * 3. Removes the main branch from the view
 * 4. Adds the development branch to the view
 * 5. Tries to rebase the feature branch on the development branch's head
 * 6. Undoes the rebase action
 * 7. Redoes the rebase action
 * 8. Resets the changes made to the feature branch
 * 9. Moves the base of the feature branch to the second to last commit on the feature branch
 * 10. Starts the rebase process
 */
class UseCase4Test : IRGitPlatformTest() {
    lateinit var secondCommitOnMain: String
    lateinit var thirdCommitOnMain: String

    var featureBranch: String = "feature"
    lateinit var firstCommitOnFeature: String
    lateinit var secondCommitOnFeature: String
    lateinit var thirdCommitOnFeature: String
    lateinit var fourthCommitOnFeature: String
    lateinit var fifthCommitOnFeature: String

    override fun setUp() {
        super.setUp()
        runBlocking(Dispatchers.IO) {
            repository.checkout("main")
            assertCorrectCheckedOutBranch("main")

            secondCommitOnMain = createAndCommitNewFile("file5.txt", "second")
            thirdCommitOnMain = createAndCommitNewFile("file6.txt", "third")

            Awaitility.await()
                .atMost(15000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until {
                    gitCommitsCountEquals(2)
                }

            repository.checkoutNew(featureBranch)
            assertCorrectCheckedOutBranch(featureBranch)

            repository.git("branch")

            firstCommitOnFeature = createAndCommitNewFile("file7.txt", "refactor")
            secondCommitOnFeature = createAndCommitNewFile("file8.txt", "whatever")
            thirdCommitOnFeature = createAndCommitNewFile("file9.txt", "it works")
            fourthCommitOnFeature = createAndCommitNewFile("file10.txt", "testy")
            fifthCommitOnFeature = createAndCommitNewFile("file11.txt", "new file")

            assertCorrectCheckedOutBranch(featureBranch)
            Awaitility.await()
                .atMost(15000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until {
                    gitCommitsCountEquals(7)
                }
        }
    }

    override fun openAndInitializePlugin(expectedCount: Int) {
        assertCorrectCheckedOutBranch(featureBranch)
        val openEditorTabAction = CreateEditorTabAction()
        val testEvent = createTestEvent(openEditorTabAction)
        assertThat(testEvent.project).isEqualTo(project)

        openEditorTabAction.actionPerformed(testEvent)

        val modelService = project.service<ModelService>()
        Awaitility.await()
            .atMost(15000, TimeUnit.MILLISECONDS)
            .pollDelay(50, TimeUnit.MILLISECONDS)
            .until { modelService.branchInfo.initialCommits.size == expectedCount }
        assertThat(modelService.branchInfo.name).isEqualTo(featureBranch)
    }

    fun testUseCase4() {
        runBlocking(Dispatchers.EDT) {
            openAndInitializePlugin(5)
            val modelService = project.service<ModelService>()

            // open the side panel
            val addBranchAction = AddBranchAction()
            val testEvent1 = createTestEvent(addBranchAction)
            addBranchAction.actionPerformed(testEvent1)

            val nav = BranchNavigationListener(project, modelService)

            val sidePanel =
                project.service<ActionService>()
                    .mainPanel.sidePanel
            val sidePanelPane = project.service<ActionService>().mainPanel.sidePanelPane
            assertThat(sidePanelPane.isVisible).isTrue()

            Awaitility.await()
                .pollInSameThread()
                .alias("open side panel and check if correct branches are displayed")
                .atMost(30000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until {
                    sidePanel.branches.equals(listOf("development", "main"))
                }

            val mainBranchPanel = sidePanel.sideBranchPanels[1]
            assertThat(mainBranchPanel.branchName).isEqualTo("main")

            // to select a branch to add to the view
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

            // check if the correct information about the 2 branches is added to the model
            val addedBranch = modelService.graphInfo.addedBranch
            Awaitility.await()
                .pollInSameThread()
                .alias("add main to view")
                .atMost(15000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until {
                    addedBranch?.name == "main"
                }

            Awaitility.await()
                .pollInSameThread()
                .alias("correct commits on main branch")
                .atMost(15000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until {
                    val commitsOnMainBranch = addedBranch?.initialCommits?.map { it.commit.subject }
                    commitsOnMainBranch == listOf("third")
                }

            assertThat(modelService.graphInfo.mainBranch.name).isEqualTo(featureBranch)
            assertThat(modelService.graphInfo.mainBranch.initialCommits.map { it.commit.subject })
                .isEqualTo(listOf("new file", "testy", "it works", "whatever", "refactor"))

            mainBranchPanelListener.mouseClicked(mouseEvent)

            Awaitility.await()
                .atMost(15000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .alias("removing a second branch from the view and refreshing")
                .pollInSameThread()
                .until {
                    modelService.graphInfo.addedBranch == null
                }

            val devBranchPanel = sidePanel.sideBranchPanels[0]
            assertThat(devBranchPanel.branchName).isEqualTo("development")

            // to select a branch to add to the view
            val devBranchPanelListener = devBranchPanel.mouseListeners[0]
            val mouseEvent2 = MouseEvent(devBranchPanel, 9, 0L, 0, 2, 2, 1, false)

            devBranchPanelListener.mouseClicked(mouseEvent2)

            Awaitility.await()
                .atMost(15000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .alias("adding dev branch to the view and refreshing")
                .pollInSameThread()
                .until {
                    modelService.graphInfo.addedBranch != null
                }

            // check if the correct information about the 2 branches is added to the model
            val devBranch = modelService.graphInfo.addedBranch

            assertThat(devBranch?.name).isEqualTo("development")

            Awaitility.await()
                .atMost(15000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .alias("check commits in dev branch are correct")
                .pollInSameThread()
                .until {
                    val commitsOnDevBranch = devBranch?.initialCommits?.map { it.commit.subject }
                    commitsOnDevBranch == listOf("my final commit", "i love testing", "code quality", "first", "initial")
                }

            assertThat(modelService.graphInfo.mainBranch.name).isEqualTo(featureBranch)
            assertThat(modelService.graphInfo.mainBranch.initialCommits.map { it.commit.subject })
                .isEqualTo(listOf("new file", "testy", "it works", "whatever", "refactor", "third", "second"))

            val changeBaseAction = RebaseAction()
            val changeBaseEvent = createTestEvent(changeBaseAction)
            changeBaseAction.update(changeBaseEvent)
            assertThat(changeBaseEvent.presentation.isEnabled).isTrue()

            //go to second branch with keyboard navigation
            nav.down()
            nav.right()
            nav.up()
            nav.up()

            val headOfSecondBranch = modelService.graphInfo.addedBranch?.selectedCommits?.get(0)
            modelService.graphInfo.addedBranch?.baseCommit = headOfSecondBranch
            project.service<ActionService>().takeNormalRebaseAction()

            var invokerCommands = project.service<RebaseInvoker>().commands.filterIsInstance<RebaseCommand>()
            assertThat(invokerCommands[0].commit).isEqualTo(headOfSecondBranch)

            //undoes the rebase action
            val undoAction = UndoAction()
            val undoEvent = createTestEvent(undoAction)
            undoAction.update(undoEvent)
            assertThat(undoEvent.presentation.isEnabled).isTrue()

            undoAction.actionPerformed(undoEvent)

            modelService.selectSingleCommit(modelService.graphInfo.addedBranch!!.currentCommits[1],
                modelService.graphInfo.addedBranch!!
            )

            //redoes the rebase action
            val redoAction = RedoAction()
            val redoEvent = createTestEvent(redoAction)
            redoAction.update(redoEvent)
            assertThat(redoEvent.presentation.isEnabled).isTrue()

            redoAction.actionPerformed(redoEvent)
            invokerCommands = project.service<RebaseInvoker>().commands.filterIsInstance<RebaseCommand>()
            assertThat(invokerCommands[0].commit).isEqualTo(headOfSecondBranch)

            //resets all changes made to the branch
            val resetAction = ResetAction()
            val resetEvent = createTestEvent(resetAction)
            resetAction.update(resetEvent)
            assertThat(resetEvent.presentation.isEnabled).isTrue()
            resetAction.actionPerformed(resetEvent)

            assertThat(project.service<RebaseInvoker>().commands.isEmpty()).isTrue()

            assertThat(resetEvent.presentation.isEnabled).isTrue()
            assertThat(resetAction.actionUpdateThread).isEqualTo(ActionUpdateThread.EDT)
            resetAction.actionPerformed(resetEvent)

            //move the base of the branch to be the second to last commit on the second branch
            val secondChangeBaseEvent = createTestEvent(changeBaseAction)
            changeBaseAction.update(secondChangeBaseEvent)
            assertThat(secondChangeBaseEvent.presentation.isEnabled).isTrue()

            //go to second branch with keyboard navigation
            nav.down()
            nav.right()
            nav.up()
            val commitOnSecondBranch = modelService.graphInfo.addedBranch?.selectedCommits?.get(0)
            println(commitOnSecondBranch?.commit?.subject)
            modelService.graphInfo.addedBranch?.baseCommit = commitOnSecondBranch
            project.service<ActionService>().takeNormalRebaseAction()

            invokerCommands = project.service<RebaseInvoker>().commands.filterIsInstance<RebaseCommand>()
            assertThat(invokerCommands[0].commit).isEqualTo(commitOnSecondBranch)

            //starts the rebase process
            val rebaseAction = StartRebaseAction()
            val rebaseEvent = createTestEvent(rebaseAction)
            assertThat(rebaseAction.actionUpdateThread).isEqualTo(ActionUpdateThread.EDT)
            rebaseAction.actionPerformed(rebaseEvent)

            //asserts that the rebase action was done, moving it further away from the initial commit,
            //3 commits to be exact
            Awaitility.await()
                .alias("rebase action being done")
                .pollInSameThread()
                .atMost(10000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until {
                    countCommitsSinceSpecificCommit(initialCommitOnMain) == 10
                }
            }
        }

}
