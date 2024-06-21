package com.jetbrains.interactiveRebase.integrationTests

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.testFramework.TestActionEvent.createTestEvent
import com.jetbrains.interactiveRebase.actions.buttonActions.StartRebaseAction
import com.jetbrains.interactiveRebase.actions.changePanel.RedoAction
import com.jetbrains.interactiveRebase.actions.changePanel.UndoAction
import com.jetbrains.interactiveRebase.actions.gitPanel.DropAction
import com.jetbrains.interactiveRebase.actions.gitPanel.FixupAction
import com.jetbrains.interactiveRebase.actions.gitPanel.PickAction
import com.jetbrains.interactiveRebase.listeners.BranchNavigationListener
import com.jetbrains.interactiveRebase.services.ModelService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.awaitility.Awaitility
import java.util.concurrent.TimeUnit

/**
 * Tests a use case in which the following actions are performed:
 * 1. Drop a commit ("my final commit")
 * 2. Fixup a commit ("i love testing" into "code quality")
 * 3. Pick a commit ("code quality") => this reverts the fixup action
 * 4. Undo the last action => undo the pick action, commits are picked
 * 5. Redo the last action => picks the commits again
 * 6. Reorder a commit => reorder "i love testing" to be the latest commit
 * 7. Rebase the branch
 *
 * It asserts at the end that all the performed actions have been executed correctly,
 * and the branch has been rebased successfully.
 */
class UseCase1Test : IRGitPlatformTest() {
    fun testUseCase1() {
        runBlocking(Dispatchers.EDT) {
            // this opens the editor tab, and initializes everything
            openAndInitializePlugin()

            val modelService = project.service<ModelService>()

            // this selects the last commit and sets it up to be dropped
            val commitToDrop = modelService.branchInfo.currentCommits[0]
            assertThat(commitToDrop.commit.subject).isEqualTo("my final commit")

            modelService.selectSingleCommit(commitToDrop, modelService.branchInfo)

            val dropAction = DropAction()
            val dropTestEvent = createTestEvent(dropAction)
            dropAction.update(dropTestEvent)
            assertThat(dropTestEvent.presentation.isEnabled).isTrue()

            dropAction.actionPerformed(dropTestEvent)

            val fixupCommit = modelService.branchInfo.currentCommits[1]
            modelService.selectSingleCommit(fixupCommit, modelService.branchInfo)

            val fixupAction = FixupAction()
            val fixupTestEvent = createTestEvent(fixupAction)
            fixupAction.update(fixupTestEvent)
            assertThat(fixupTestEvent.presentation.isEnabled).isTrue()

            fixupAction.actionPerformed(fixupTestEvent)

            val parentOfFixedUpCommit = modelService.branchInfo.currentCommits[1]
            modelService.selectSingleCommit(parentOfFixedUpCommit, modelService.branchInfo)

            val pickAction = PickAction()
            val pickTestEvent = createTestEvent(pickAction)
            pickAction.update(pickTestEvent)
            assertThat(pickTestEvent.presentation.isEnabled).isTrue()

            pickAction.actionPerformed(pickTestEvent)
            assertThat(modelService.branchInfo.currentCommits.size).isEqualTo(4)

            val undoAction = UndoAction()
            val undoTestEvent = createTestEvent(undoAction)
            undoAction.actionPerformed(undoTestEvent)

            assertThat(modelService.branchInfo.currentCommits.size).isEqualTo(3)

            val redoAction = RedoAction()
            val redoTestEvent = createTestEvent(redoAction)
            redoAction.actionPerformed(redoTestEvent)

            assertThat(modelService.branchInfo.currentCommits.size).isEqualTo(4)

            val reorderCommit = modelService.branchInfo.currentCommits[1]
            modelService.selectSingleCommit(reorderCommit, modelService.branchInfo)
            val keyboardShortcut = BranchNavigationListener(project)
            keyboardShortcut.altDown()
            keyboardShortcut.altDown()

            assertThat(
                modelService.branchInfo.currentCommits.map {
                    it.commit.subject
                },
            ).isEqualTo(listOf("my final commit", "code quality", "first", "i love testing"))

            val rebaseAction = StartRebaseAction()
            val rebaseEvent = createTestEvent(rebaseAction)
            rebaseAction.actionPerformed(rebaseEvent)

            Awaitility.await()
                .alias("3 commits left for drop")
                .pollInSameThread()
                .atMost(20000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until {
                    gitCommitsCountEquals(3)
                }
            var remainingCommitMessages = repository.getAllCommitMessages()
            remainingCommitMessages = remainingCommitMessages.filter { it != "initial" }

            assertThat(remainingCommitMessages.contains("my final commit")).isFalse()
            assertThat(remainingCommitMessages).isEqualTo(listOf("code quality", "first", "i love testing"))
        }
    }
}
