package com.jetbrains.interactiveRebase.integrationTests

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.testFramework.TestActionEvent.createTestEvent
import com.jetbrains.interactiveRebase.actions.gitPanel.StopToEditAction
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.git
import com.jetbrains.interactiveRebase.services.ModelService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.awaitility.Awaitility
import java.util.concurrent.TimeUnit

class StopToEditActionTest : IRGitPlatformTest() {
    fun testStopToEditCommit() {
        runBlocking(Dispatchers.EDT) {
            // this opens the editor tab, and initializes everything
            openAndInitializePlugin()
            val modelService = project.service<ModelService>()

            // this selects the second-to-last commit and sets it up to be edited
            val commitToEdit = modelService.branchInfo.currentCommits[1]
            assertThat(commitToEdit.commit.subject).isEqualTo("i love testing")
            modelService.selectSingleCommit(commitToEdit, modelService.branchInfo)

            val editAction = StopToEditAction()
            val testEvent1 = createTestEvent(editAction)
            editAction.actionPerformed(testEvent1)

            // this clicks the rebase button
            val rebaseButton = getRebaseButton()
            rebaseButton.doClick()

            Awaitility.await()
                .atMost(15000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .alias("status is rebase in process")
                .pollInSameThread()
                .until {
                    val statusOutput = repository.git("status")
                    statusOutput.contains("rebase in process")
                    statusOutput.contains("i love testing")
                }

            // this continues the rebase
            repository.git("rebase --continue")

            // this checks that the rebase was continued and finished
            Awaitility.await()
                .atMost(20000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .alias("no longer stopped to edit")
                .pollInSameThread()
                .until {
                    val statusOutput = repository.git("status")
                    statusOutput.contains("rebase in process").not()
                }

            assertThat(gitCommitsCountEquals(4)).isTrue()
            val remainingCommitMessages = repository.getAllCommitMessages()
            assertThat(remainingCommitMessages.containsAll(listOf("first", "code quality", "i love testing", "my final commit"))).isTrue()
        }
    }
}
