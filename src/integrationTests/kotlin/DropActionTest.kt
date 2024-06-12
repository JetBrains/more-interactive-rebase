package com.jetbrains.interactiveRebase.integrationTests

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.testFramework.TestActionEvent.createTestEvent
import com.jetbrains.interactiveRebase.actions.gitPanel.DropAction
import com.jetbrains.interactiveRebase.services.ModelService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.awaitility.Awaitility
import java.util.concurrent.TimeUnit

class DropActionTest : IRGitPlatformTest() {
    fun testDropCommit() {
        runBlocking(Dispatchers.EDT) {
            // this opens the editor tab, and initializes everything
            openAndInitializePlugin()

            val modelService = project.service<ModelService>()

            // this selects the last commit and sets it up to be dropped
            val commitToDrop = modelService.branchInfo.currentCommits[0]
            assertThat(commitToDrop.commit.subject).isEqualTo("my final commit")

            modelService.selectSingleCommit(commitToDrop, modelService.branchInfo)

            val dropAction = DropAction()
            val testEvent1 = createTestEvent(dropAction)
            dropAction.actionPerformed(testEvent1)

            // this clicks the rebase button
            val rebaseButton = getRebaseButton()
            rebaseButton.doClick()

            Awaitility.await()
                .alias("3 commits left for drop")
                .pollInSameThread()
                .atMost(20000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until {
                    gitCommitsCountEquals(3)
                }
            val remainingCommitMessages = repository.getAllCommitMessages()
            assertThat(remainingCommitMessages.contains("my final commit")).isFalse()
            assertThat(remainingCommitMessages.containsAll(listOf("first", "code quality", "i love testing"))).isTrue()
        }
    }
}
