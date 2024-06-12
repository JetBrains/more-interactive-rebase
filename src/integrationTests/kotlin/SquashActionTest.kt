package com.jetbrains.interactiveRebase.integrationTests

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.testFramework.TestActionEvent.createTestEvent
import com.jetbrains.interactiveRebase.actions.gitPanel.SquashAction
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.git
import com.jetbrains.interactiveRebase.listeners.TextFieldListener
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.ModelService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.awaitility.Awaitility
import java.util.concurrent.TimeUnit

class SquashActionTest : IRGitPlatformTest() {
    fun testSquashCommits() {
        runBlocking(Dispatchers.EDT) {
            // this opens the editor tab, and initializes everything
            openAndInitializePlugin()
            val modelService = project.service<ModelService>()

            // this selects the last commit ("please work") and sets it up to be squashed
            val commitToSquash = modelService.branchInfo.currentCommits[0]
            assertThat(commitToSquash.commit.subject).isEqualTo("my final commit")
            modelService.selectSingleCommit(commitToSquash, modelService.branchInfo)

            // this selects the third-to-last commit ("IMHO") and sets it up to be squashed
            val commitToSquashInto = modelService.branchInfo.currentCommits[2]
            assertThat(commitToSquashInto.commit.subject).isEqualTo("code quality")
            modelService.addToSelectedCommits(commitToSquashInto, modelService.branchInfo)

            // this "sets up" the commits to be squashed, by enabling the text field
            val squashAction = SquashAction()
            val testEvent1 = createTestEvent(squashAction)
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

            // here we click the rebase button
            val rebaseButton = getRebaseButton()
            rebaseButton.doClick()

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
