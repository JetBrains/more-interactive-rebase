package com.jetbrains.interactiveRebase.integrationTests

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.testFramework.TestActionEvent.createTestEvent
import com.jetbrains.interactiveRebase.actions.buttonActions.StartRebaseAction
import com.jetbrains.interactiveRebase.actions.changePanel.RedoAction
import com.jetbrains.interactiveRebase.actions.changePanel.UndoAction
import com.jetbrains.interactiveRebase.actions.gitPanel.PickAction
import com.jetbrains.interactiveRebase.actions.gitPanel.SquashAction
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.git
import com.jetbrains.interactiveRebase.listeners.BranchNavigationListener
import com.jetbrains.interactiveRebase.listeners.CircleHoverListener
import com.jetbrains.interactiveRebase.listeners.TextFieldListener
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.ModelService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.awaitility.Awaitility
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import java.util.concurrent.TimeUnit

/**
 * This test has to do with squashing mostly.
 *
 */
class UseCase3Test : IRGitPlatformTest() {
    fun testUseCase3() {
        runBlocking(Dispatchers.EDT) {
            // this opens the editor tab, and initializes everything
            openAndInitializePlugin()
            val modelService = project.service<ModelService>()

            val nav = BranchNavigationListener(project, modelService)

            // this selects the last commit ("my final commit") and sets it up to be squashed
            nav.shiftDown()
            val commitToSquash = modelService.branchInfo.currentCommits[0]
            assertThat(commitToSquash.commit.subject).isEqualTo("my final commit")

            //perform a control click having the first commit already selected and clicking the third commit
            val ctrlClik = CircleHoverListener(project.service<ActionService>()
                .mainPanel.graphPanel.mainBranchPanel.branchPanel.circles[2])
            val clickEvent = MouseEvent(ctrlClik.circlePanel, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                InputEvent.CTRL_DOWN_MASK, 0, 0, 1, false)
            ctrlClik.mouseClicked(clickEvent)
            assertThat(modelService.getSelectedCommits().size).isEqualTo(2)

            // this "sets up" the commits to be squashed, by enabling the text field
            val squashAction = SquashAction()
            val testEvent1 = createTestEvent(squashAction)
            squashAction.update(testEvent1)
            assertThat(testEvent1.presentation.isEnabled).isTrue()
            squashAction.actionPerformed(testEvent1)

            // here we pretend that we are a user inputting the data new commit message
            // in the GUI, by getting the listener and setting the text field to the new message
            val labeledBranchPanel = project.service<ActionService>().mainPanel.graphPanel.mainBranchPanel
            val textField = labeledBranchPanel.getTextField(1)

            val listener = textField.keyListeners[0] as TextFieldListener
            assertThat(listener).isNotNull()

            listener.textField.text = "squyshy"

            // here we pretend we pressed enter after typing the new message
            listener.processEnter()

            // here we perform a pick action on the second commit, after going through the commits with the arrow keys
            val pickAction = PickAction()
            val testEvent2 = createTestEvent(pickAction)
            nav.down()
            nav.down()
            nav.down()
            nav.up()
            pickAction.update(testEvent2)

            assertThat(testEvent2.presentation.isEnabled).isTrue()
            pickAction.actionPerformed(testEvent2)
            assertThat(modelService.branchInfo.currentCommits.size).isEqualTo(4)

            // here we perform a undo action on the third commit
            val undoAction = UndoAction()
            val testEvent3 = createTestEvent(undoAction)
            val commitToUndo = modelService.branchInfo.currentCommits[2]
            assertThat(commitToUndo.commit.subject).isEqualTo("code quality")
            undoAction.actionPerformed(testEvent3)
            assertThat(modelService.branchInfo.currentCommits.size).isEqualTo(3)

            //here we undo again
            val testEvent4 = createTestEvent(undoAction)
            undoAction.actionPerformed(testEvent4)
            assertThat(modelService.branchInfo.currentCommits.size).isEqualTo(4)

            // here we perform a redo action on the third commit, which should make the commits be squashed again
            val redoAction = RedoAction()
            val testEvent5 = createTestEvent(redoAction)
            redoAction.actionPerformed(testEvent5)
            assertThat(modelService.branchInfo.currentCommits.size).isEqualTo(3)

            // here we click the rebase button
            val rebaseAction = StartRebaseAction()
            val rebaseEvent = createTestEvent(rebaseAction)
            rebaseAction.actionPerformed(rebaseEvent)

            Awaitility.await()
                .atMost(20000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .alias("squash commit message changed")
                .pollInSameThread()
                .until {
                    val commitMessage = repository.git("log --format=%B -n 1 HEAD~1")
                    commitMessage.equals("squyshy\n")
                }

            assertThat(gitCommitsCountEquals(3)).isTrue()
            val remainingCommitMessages = repository.getAllCommitMessages()
            assertThat(remainingCommitMessages).isEqualTo(listOf("i love testing", "squyshy", "first", "initial"))

            val parentCommitOfSquashedWithNewHash = git("rev-parse HEAD~1")
            val fileChangesOfParent = git("show --name-only $parentCommitOfSquashedWithNewHash")
            assertThat(fileChangesOfParent).contains("file2.txt")
            assertThat(fileChangesOfParent).doesNotContain("file1.txt")
        }
    }
}
