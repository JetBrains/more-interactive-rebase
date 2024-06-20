package com.jetbrains.interactiveRebase.integrationTests

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.testFramework.TestActionEvent.createTestEvent
import com.jetbrains.interactiveRebase.actions.buttonActions.StartRebaseAction
import com.jetbrains.interactiveRebase.actions.gitPanel.FixupAction
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.git
import com.jetbrains.interactiveRebase.services.ModelService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.awaitility.Awaitility
import java.util.concurrent.TimeUnit

class FixupActionTest : IRGitPlatformTest() {
    fun testFixupCommit() {
        runBlocking(Dispatchers.EDT) {
            // this opens the editor tab, and initializes everything
            openAndInitializePlugin()
            val modelService = project.service<ModelService>()

            // in the case where only 1 commit is selected
            val commitToSquash = modelService.branchInfo.currentCommits[1]
            assertThat(commitToSquash.commit.subject).isEqualTo("i love testing")
            modelService.selectSingleCommit(commitToSquash, modelService.branchInfo)

            // this selects the last commit and sets it up to be fixed up with its previous commit
            val fixupAction = FixupAction()
            val testEvent1 = createTestEvent()
            fixupAction.actionPerformed(testEvent1)

            // this clicks the rebase button
            val rebaseAction = StartRebaseAction()
            val rebaseEvent = createTestEvent(rebaseAction)
            rebaseAction.actionPerformed(rebaseEvent)

            Awaitility.await()
                .alias("3 commits left for fixup")
                .pollInSameThread()
                .atMost(20000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until {
                    gitCommitsCountEquals(3)
                }
            val remainingCommitMessages = repository.getAllCommitMessages()
            assertThat(remainingCommitMessages.contains("i love testing")).isFalse()
            assertThat(remainingCommitMessages.containsAll(listOf("first", "code quality", "my final commit"))).isTrue()

            // assert that the fixup commit also has the changes of the commit that was squashed
            val parentCommitOfSquashedWithNewHash = git("rev-parse HEAD~1")
            val fileChangesOfParent = git("show --name-only $parentCommitOfSquashedWithNewHash")
            assertThat(fileChangesOfParent).contains("file3.txt")
            assertThat(fileChangesOfParent).contains("file2.txt")
        }
    }
}
