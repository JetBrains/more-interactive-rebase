
import com.intellij.openapi.components.service
import com.intellij.testFramework.TestActionEvent.createTestEvent
import com.jetbrains.interactiveRebase.actions.CreateEditorTabAction
import com.jetbrains.interactiveRebase.actions.gitPanel.DropAction
import com.jetbrains.interactiveRebase.actions.gitPanel.FixupAction
import com.jetbrains.interactiveRebase.actions.gitPanel.StopToEditAction
import com.jetbrains.interactiveRebase.integrationTests.IRGitPlatformTest
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.TestFile
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.addCommit
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.git
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.visuals.RoundedButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
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
    }

    fun testDropCommit() = runTest{
        launch(Dispatchers.Main){
        val openEditorTabAction = CreateEditorTabAction()
        val testEvent = createTestEvent()
        openEditorTabAction.actionPerformed(testEvent)


        val modelService = project.service<ModelService>()
            withContext(Dispatchers.IO) {
                sleep(1000)
            }
        assertThat(modelService.branchInfo.currentCommits).hasSize(4)

        val commitToDrop = modelService.branchInfo.currentCommits[0]

        commitToDrop.isSelected = true
        modelService.addOrRemoveCommitSelection(commitToDrop)

        val dropAction = DropAction()
        val testEvent1 = createTestEvent()
        dropAction.actionPerformed(testEvent1)

        val headerPanel = project.service<ActionService>().getHeaderPanel()
        val changesActionsPanel = headerPanel.changeActionsPanel
        val rebaseButton = changesActionsPanel.components[1] as RoundedButton
        rebaseButton.doClick()

            withContext(Dispatchers.IO) {
                sleep(1000)
            }
        assertThat(modelService.branchInfo.currentCommits).hasSize(3)
        assertThat(modelService.branchInfo.currentCommits).doesNotContain(commitToDrop)

        val leftOverCommits = countCommitsSinceInitialCommit()
        assertThat(leftOverCommits).isEqualTo(3)}
    }

    fun testFixupCommit() = runTest{
        launch(Dispatchers.Main){
            val openEditorTabAction = CreateEditorTabAction()
            val testEvent = createTestEvent()
            openEditorTabAction.actionPerformed(testEvent)
        val modelService = project.service<ModelService>()
            withContext(Dispatchers.IO) {
                sleep(1000)
            }
        assertThat(modelService.branchInfo.currentCommits).hasSize(4)

        // in the case where only 1 commit is selected
        val commitToSquash = modelService.branchInfo.currentCommits[1]
        commitToSquash.isSelected = true
        modelService.addOrRemoveCommitSelection(commitToSquash)

        val fixupAction = FixupAction()
        val testEvent1 = createTestEvent()
        fixupAction.actionPerformed(testEvent1)

        val headerPanel = project.service<ActionService>().getHeaderPanel()
        val changesActionsPanel = headerPanel.changeActionsPanel
        val rebaseButton = changesActionsPanel.components[1] as RoundedButton
        rebaseButton.doClick()

            withContext(Dispatchers.IO) {
                sleep(1000)
            }

        val leftOverCommits = countCommitsSinceInitialCommit()
        assertThat(leftOverCommits).isEqualTo(3)}
    }

    fun testStopToEditCommit() =  runTest{
        launch(Dispatchers.Main){
            val openEditorTabAction = CreateEditorTabAction()
            val testEvent = createTestEvent()
            openEditorTabAction.actionPerformed(testEvent)

        val modelService = project.service<ModelService>()
            withContext(Dispatchers.IO) {
                sleep(1000)
            }
        assertThat(modelService.branchInfo.currentCommits).hasSize(4)

        // in the case where only 1 commit is selected
        val commitToEdit = modelService.branchInfo.currentCommits[1]
        commitToEdit.isSelected = true
        modelService.addOrRemoveCommitSelection(commitToEdit)

        val editAction = StopToEditAction()
        val testEvent1 = createTestEvent()
        editAction.actionPerformed(testEvent1)

        val headerPanel = project.service<ActionService>().getHeaderPanel()
        val changesActionsPanel = headerPanel.changeActionsPanel
        val rebaseButton = changesActionsPanel.components[1] as RoundedButton
        rebaseButton.doClick()

            withContext(Dispatchers.IO) {
                sleep(1000)
            }
        val statusOutput = repository.git("status")
        assertThat(statusOutput).contains("edit " + commit3.substring(0, 7) + " Cool stuff")

        repository.git("rebase --continue")

        sleep(1000)
        val leftOverCommits = countCommitsSinceInitialCommit()
        assertThat(leftOverCommits).isEqualTo(4)
    }}

    fun countCommitsSinceInitialCommit(): Int {
        val result = repository.git("rev-list --count " + initialCommit + "..HEAD")
        return result.toInt()
    }
}