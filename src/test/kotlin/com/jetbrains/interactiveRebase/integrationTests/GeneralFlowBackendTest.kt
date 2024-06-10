
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.testFramework.TestActionEvent.createTestEvent
import com.jetbrains.interactiveRebase.actions.CreateEditorTabAction
import com.jetbrains.interactiveRebase.actions.gitPanel.DropAction
import com.jetbrains.interactiveRebase.actions.gitPanel.FixupAction
import com.jetbrains.interactiveRebase.actions.gitPanel.RewordAction
import com.jetbrains.interactiveRebase.actions.gitPanel.SquashAction
import com.jetbrains.interactiveRebase.actions.gitPanel.StopToEditAction
import com.jetbrains.interactiveRebase.integrationTests.IRGitPlatformTest
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.TestFile
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.addCommit
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.git
import com.jetbrains.interactiveRebase.listeners.TextFieldListener
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.visuals.RoundedButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.assertj.core.api.Assertions.assertThat
import java.io.File
import java.lang.Thread.sleep

class GeneralFlowBackendTest : IRGitPlatformTest() {
    lateinit var commit2: String
    lateinit var commit3: String
    lateinit var commit4: String
    lateinit var file2: TestFile

    override fun setUp() {
        super.setUp()
        file2 = TestFile(repository, File(projectRoot.path, "file2.txt"))
        file2.write("i think this is cool")
        commit2 = addCommit("IMHO")

        file1.append("I really like testing")
        commit3 = addCommit("Cool stuff")

        file2.write("bruuuuuh")
        commit4 = addCommit("please work")
        sleep(1500)
    }

    fun testDropCommit() {
        runBlocking(Dispatchers.EDT) {
            // this opens the editor tab, and initializes everything
            val openEditorTabAction = CreateEditorTabAction()
            val testEvent = createTestEvent()
            openEditorTabAction.actionPerformed(testEvent)

            // this gets the current commits of the checked out branch
            val modelService = project.service<ModelService>()

            sleep(1000)
            assertThat(modelService.branchInfo.name).isEqualTo("development")
            assertThat(modelService.branchInfo.currentCommits).hasSize(4)

            // this selects the last commit and sets it up to be dropped
            val commitToDrop = modelService.branchInfo.currentCommits[0]
            modelService.addToSelectedCommits(commitToDrop, modelService.branchInfo)

            val dropAction = DropAction()
            val testEvent1 = createTestEvent()
            dropAction.actionPerformed(testEvent1)

            // this clicks the rebase button
            val headerPanel = project.service<ActionService>().getHeaderPanel()
            val changesActionsPanel = headerPanel.changeActionsPanel
            val rebaseButton = changesActionsPanel.components[1] as RoundedButton
            rebaseButton.doClick()

            sleep(2000)
            // this asserts that the commit was dropped both in our model and in the git repository
            assertThat(modelService.branchInfo.currentCommits).hasSize(3)
            assertThat(modelService.branchInfo.currentCommits).doesNotContain(commitToDrop)

            val leftOverCommits = countCommitsSinceInitialCommit()
            assertThat(leftOverCommits).isEqualTo(3)
        }
    }

    fun testFixupCommit() {
        runBlocking(Dispatchers.EDT) {
            // this opens the editor tab, and initializes everything
            val openEditorTabAction = CreateEditorTabAction()
            val testEvent = createTestEvent()
            openEditorTabAction.actionPerformed(testEvent)

            // this gets the current commits of the checked out branch
            val modelService = project.service<ModelService>()
            sleep(1000)

            assertThat(modelService.branchInfo.name).isEqualTo("development")
            assertThat(modelService.branchInfo.currentCommits).hasSize(4)
                // in the case where only 1 commit is selected
                val commitToSquash = modelService.branchInfo.currentCommits[1]
                modelService.addToSelectedCommits(commitToSquash, modelService.branchInfo)

            // this selects the last commit and sets it up to be fixed up with its previous commit
            val fixupAction = FixupAction()
            val testEvent1 = createTestEvent()
            fixupAction.actionPerformed(testEvent1)

            // this clicks the rebase button
            val headerPanel = project.service<ActionService>().getHeaderPanel()
            val changesActionsPanel = headerPanel.changeActionsPanel
            val rebaseButton = changesActionsPanel.components[1] as RoundedButton
            rebaseButton.doClick()

            sleep(1200)

            val leftOverCommits = countCommitsSinceInitialCommit()
            // this asserts that the commit is no longer in the model and that the commits were fixed up
            assertThat(leftOverCommits).isEqualTo(3)
        }
    }

    fun testStopToEditCommit() {
        runBlocking(Dispatchers.EDT) {
            // this opens the editor tab, and initializes everything
            val openEditorTabAction = CreateEditorTabAction()
            val testEvent = createTestEvent()
            openEditorTabAction.actionPerformed(testEvent)

            val modelService = project.service<ModelService>()
            sleep(1000)


            assertThat(modelService.branchInfo.name).isEqualTo("development")
            assertThat(modelService.branchInfo.currentCommits).hasSize(4)

                // this selects the second-to-last commit and sets it up to be edited
                val commitToEdit = modelService.branchInfo.currentCommits[1]
                modelService.addToSelectedCommits(commitToEdit, modelService.branchInfo)


            val editAction = StopToEditAction()
            val testEvent1 = createTestEvent()
            editAction.actionPerformed(testEvent1)

            // this clicks the rebase button
            val headerPanel = project.service<ActionService>().getHeaderPanel()
            val changesActionsPanel = headerPanel.changeActionsPanel
            val rebaseButton = changesActionsPanel.components[1] as RoundedButton
            rebaseButton.doClick()

            withContext(Dispatchers.IO) {
                sleep(1000)
            }
            // this checks that the rebase was paused
            val statusOutput = repository.git("status")

            sleep(1000)

            assertThat(statusOutput).contains("edit " + commit3.substring(0, 7) + " Cool stuff")

            // this continues the rebase
            repository.git("rebase --continue")

                sleep(1000)
            // this checks that the rebase was continued and finished
            val leftOverCommits = countCommitsSinceInitialCommit()
            assertThat(leftOverCommits).isEqualTo(4)
        }
    }

    fun testRewordCommit() {
        runBlocking(Dispatchers.EDT) {
            // this opens the editor tab, and initializes everything
            val openEditorTabAction = CreateEditorTabAction()
            val testEvent = createTestEvent()
            openEditorTabAction.actionPerformed(testEvent)
            sleep(1000)

            // this gets the current commits of the checked out branch
            val modelService = project.service<ModelService>()
            sleep(1000)

            assertThat(modelService.branchInfo.name).isEqualTo("development")
            assertThat(modelService.branchInfo.currentCommits).hasSize(4)

            // this "sets up" the commit to be reworded, by enabling the text field
            val rewordAction = RewordAction()
            val testEvent1 = createTestEvent()
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
            val headerPanel = project.service<ActionService>().getHeaderPanel()
            val changesActionsPanel = headerPanel.changeActionsPanel
            val rebaseButton = changesActionsPanel.components[1] as RoundedButton
            rebaseButton.doClick()

            sleep(1000)

            // this gets the second-to-last commit message (as the commit hash has changed)
            val commitMessage = repository.git("log --format=%B -n 1 HEAD~1")

            sleep(300)

            // here we check if the commit message is the one we set
            assertThat(commitMessage).isEqualTo("I swear this is reworded:)\n")
        }
    }

    fun testSquashCommits() {
        runBlocking(Dispatchers.EDT) {
            // this opens the editor tab, and initializes everything
            val openEditorTabAction = CreateEditorTabAction()
            val testEvent = createTestEvent()
            openEditorTabAction.actionPerformed(testEvent)

            // this gets the current commits of the checked out branch
            val modelService = project.service<ModelService>()
            sleep(1000)

            assertThat(modelService.branchInfo.name).isEqualTo("development")
            assertThat(modelService.branchInfo.currentCommits).hasSize(4)

            // this selects the last commit ("please work") and sets it up to be squashed
            val commitToSquash = modelService.branchInfo.currentCommits[0]
            commitToSquash.isSelected = true
            modelService.addToSelectedCommits(commitToSquash, modelService.branchInfo)

            // this selects the third-to-last commit ("IMHO") and sets it up to be squashed
            val commitToSquashInto = modelService.branchInfo.currentCommits[2]
            commitToSquashInto.isSelected = true
            modelService.addToSelectedCommits(commitToSquashInto, modelService.branchInfo)

            // this "sets up" the commits to be squashed, by enabling the text field
            val squashAction = SquashAction()
            val testEvent1 = createTestEvent()
            squashAction.actionPerformed(testEvent1)

            // here we pretend that we are a user inputting the data new commit message
            // in the GUI, by getting the listener and setting the text field to the new message
            val labeledBranchPanel = project.service<ActionService>().mainPanel.mainBranchPanel
            val textField = labeledBranchPanel.getTextField(1)

            val listener = textField.keyListeners[0] as TextFieldListener
            assertThat(listener).isNotNull()

            listener.textField.text = "squyshy"

            // here we pretend we pressed enter after typing the new message
            listener.processEnter()

            // here we click the rebase button
            val headerPanel = project.service<ActionService>().getHeaderPanel()
            val changesActionsPanel = headerPanel.changeActionsPanel
            val rebaseButton = changesActionsPanel.components[1] as RoundedButton
            rebaseButton.doClick()

            sleep(1000)

            // this gets the second-to-last commit message (as the commit hash has changed)
            val commitMessage = repository.git("log --format=%B -n 1 HEAD~1")
            sleep(300)
            assertThat(commitMessage).isEqualTo("squyshy\n")

            val leftOverCommits = countCommitsSinceInitialCommit()
            assertThat(leftOverCommits).isEqualTo(3)
        }
    }

    fun countCommitsSinceInitialCommit(): Int {
        val result = repository.git("rev-list --count " + initialCommit + "..HEAD")
        return result.toInt()
    }
}
