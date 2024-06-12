package com.jetbrains.interactiveRebase.integrationTests

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.testFramework.TestActionEvent.createTestEvent
import com.jetbrains.interactiveRebase.actions.gitPanel.RewordAction
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.git
import com.jetbrains.interactiveRebase.listeners.TextFieldListener
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.ModelService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.awaitility.Awaitility
import java.util.concurrent.TimeUnit

class RewordActionTest : IRGitPlatformTest() {
    fun testRewordCommit() {
        runBlocking(Dispatchers.EDT) {
            // this opens the editor tab, and initializes everything
            openAndInitializePlugin()
            val modelService = project.service<ModelService>()

            // this selects the second-to-last commit
            val commitToEdit = modelService.branchInfo.currentCommits[1]
            assertThat(commitToEdit.commit.subject).isEqualTo("i love testing")
            modelService.selectSingleCommit(commitToEdit, modelService.branchInfo)

            // this "sets up" the commit to be reworded, by enabling the text field
            val rewordAction = RewordAction()
            val testEvent1 = createTestEvent(rewordAction)
            rewordAction.actionPerformed(testEvent1)

            // here we pretend that we are a user inputting the data new commit message
            // in the GUI, by getting the listener and setting the text field to the new message
            val labeledBranchPanel = project.service<ActionService>().mainPanel.graphPanel.mainBranchPanel
            val textField = labeledBranchPanel.getTextField(1)

            val listener = textField.keyListeners[0] as TextFieldListener
            assertThat(listener).isNotNull()

            listener.textField.text = "I swear this is reworded:)"

            // here we pretend we pressed enter after typing the new message
            listener.processEnter()

            // here we click the rebase button
            val rebaseButton = getRebaseButton()
            rebaseButton.doClick()

            Awaitility.await()
                .atMost(20000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .alias("message is reworded")
                .pollInSameThread()
                .until {
                    val commitMessage = repository.git("log --format=%B -n 1 HEAD~1")
                    commitMessage.equals("I swear this is reworded:)\n")
                }

            assertThat(gitCommitsCountEquals(4)).isTrue()
            val remainingCommitMessages = repository.getAllCommitMessages()
            assertThat(
                remainingCommitMessages.containsAll(listOf("first", "code quality", "I swear this is reworded:)", "my final commit")),
            ).isTrue()
        }
    }
}
