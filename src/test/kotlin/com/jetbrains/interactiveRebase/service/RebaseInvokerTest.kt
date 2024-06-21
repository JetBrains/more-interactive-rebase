package com.jetbrains.interactiveRebase.service

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.GraphInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.*
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import com.jetbrains.interactiveRebase.utils.gitUtils.IRGitRebaseUtils
import com.jetbrains.interactiveRebase.visuals.CommitInfoPanel
import com.jetbrains.interactiveRebase.visuals.GraphPanel
import com.jetbrains.interactiveRebase.visuals.MainPanel
import git4ideaClasses.GitRebaseEntryGeneratedUsingLog
import git4ideaClasses.IRGitModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.verify

class RebaseInvokerTest : BasePlatformTestCase() {
    private lateinit var commit1: CommitInfo
    private lateinit var commit2: CommitInfo
    private lateinit var commit3: CommitInfo
    private lateinit var commit4: CommitInfo
    private lateinit var commit5: CommitInfo
    private lateinit var commit6: CommitInfo
    private lateinit var commitProvider: TestGitCommitProvider
    private lateinit var mainPanel: MainPanel
    private lateinit var modelService: ModelService
    private lateinit var branchInfo: BranchInfo
    private lateinit var rebaseInvoker: RebaseInvoker


    override fun setUp() {
        super.setUp()
        commitProvider = TestGitCommitProvider(project)
        commit1 = CommitInfo(commitProvider.createCommit("commit1"), project)
        commit2 = CommitInfo(commitProvider.createCommit("commit2"), project)
        commit3 = CommitInfo(commitProvider.createCommit("commit3"), project)
        commit4 = CommitInfo(commitProvider.createCommit("commit4"), project)
        commit5 = CommitInfo(commitProvider.createCommitWithParent("commit5", "commit6"), project)
        commit6 = CommitInfo(commitProvider.createCommitWithParent("commit5", "commit7"), project)
        rebaseInvoker = RebaseInvoker(project)
        val commitService = Mockito.mock(CommitService::class.java)
        Mockito.doAnswer {
            listOf(commit1.commit)
        }.`when`(commitService).getCommits(ArgumentMatchers.anyString())
        Mockito.doAnswer {
            "feature1"
        }.`when`(commitService).getBranchName()

        modelService = ModelService(project, CoroutineScope(Dispatchers.EDT), commitService)
        modelService.branchInfo.initialCommits = mutableListOf(commit1, commit2, commit3, commit4, commit5)
        modelService.branchInfo.currentCommits = mutableListOf(commit1, commit2, commit3, commit4, commit5)
        modelService.graphInfo = GraphInfo(modelService.branchInfo)

        modelService.branchInfo.setName("feature1")
        modelService.invoker.branchInfo = modelService.branchInfo

        rebaseInvoker.commitsToDisplayDuringRebase = mutableListOf()
        branchInfo = modelService.branchInfo
        mainPanel = MainPanel(project)
        mainPanel.commitInfoPanel = Mockito.mock(CommitInfoPanel::class.java)
        mainPanel.graphPanel = GraphPanel(project, modelService.graphInfo)
        Mockito.doNothing().`when`(mainPanel.commitInfoPanel).commitsSelected(anyCustom())
        Mockito.doNothing().`when`(mainPanel.commitInfoPanel).repaint()
        var utils = Mockito.mock(IRGitRebaseUtils::class.java)
        Mockito.doNothing().`when`(utils).rebase(anyCustom(), anyCustom())
        rebaseInvoker.gitUtils = utils
    }

    fun testAddCommand() {

        val dropCommand = Mockito.mock(DropCommand::class.java)
        rebaseInvoker.addCommand(dropCommand)
        assertTrue(rebaseInvoker.commands.size == 1)
    }

    fun testRemoveCommand() {

        val pickCommand = Mockito.mock(PickCommand::class.java)
        rebaseInvoker.commands = mutableListOf(pickCommand)
        rebaseInvoker.removeCommand(pickCommand)
        assertTrue(rebaseInvoker.commands.isEmpty())
    }

    fun testExpandListSquash() {
        // setup for squashed commits
        val squashCommand = SquashCommand(commit4, mutableListOf(commit1, commit3), "lol")
        commit4.addChange(squashCommand)

        rebaseInvoker.branchInfo.currentCommits = mutableListOf(commit2, commit4, commit5)
        rebaseInvoker.expandCurrentCommitsForSquashed()
        assertThat(rebaseInvoker.branchInfo.currentCommits.size).isEqualTo(5)
        assertThat(rebaseInvoker.branchInfo.currentCommits).isEqualTo(mutableListOf(commit2, commit1, commit3, commit4, commit5))
    }

    fun testExpandListFixupAndSquash() {
        // setup for squashed commits
        val squashCommand = SquashCommand(commit4, mutableListOf(commit1, commit3), "lol")
        commit4.addChange(squashCommand)

        commit5.addChange(FixupCommand(commit5, mutableListOf(commit2)))
        rebaseInvoker.branchInfo.currentCommits = mutableListOf(commit4, commit5)
        rebaseInvoker.expandCurrentCommitsForSquashed()
        assertThat(rebaseInvoker.branchInfo.currentCommits.size).isEqualTo(5)
        assertThat(rebaseInvoker.branchInfo.currentCommits).isEqualTo(
            mutableListOf(
                commit1,
                commit3,
                commit4,
                commit2,
                commit5,
            ),
        )
    }

    fun testCreateModel() {

        rebaseInvoker.branchInfo.currentCommits = mutableListOf(commit2, commit4, commit5)
        rebaseInvoker.createModel()
        assertThat(rebaseInvoker.model.elements[0].index).isEqualTo(0)
        assertThat(rebaseInvoker.model.elements[0].type).isEqualTo(IRGitModel.Type.NonUnite.KeepCommit.Pick)
        assertThat(rebaseInvoker.model.elements[0].entry).isEqualTo(GitRebaseEntryGeneratedUsingLog(commit5.commit))
        assertThat(rebaseInvoker.model.elements[1].index).isEqualTo(1)
        assertThat(rebaseInvoker.model.elements[1].type).isEqualTo(IRGitModel.Type.NonUnite.KeepCommit.Pick)
        assertThat(rebaseInvoker.model.elements[1].entry).isEqualTo(GitRebaseEntryGeneratedUsingLog(commit4.commit))
        assertThat(rebaseInvoker.model.elements[2].index).isEqualTo(2)
        assertThat(rebaseInvoker.model.elements[2].type).isEqualTo(IRGitModel.Type.NonUnite.KeepCommit.Pick)
        assertThat(rebaseInvoker.model.elements[2].entry).isEqualTo(GitRebaseEntryGeneratedUsingLog(commit2.commit))
    }

    fun testExecuteCommands(){
        rebaseInvoker.branchInfo = branchInfo
        val message = "commit2"
        rebaseInvoker.commands = mutableListOf(ReorderCommand(commit1,0,4),
                DropCommand(commit2), RewordCommand(commit3, message))
        rebaseInvoker.executeCommands()
        assertThat(rebaseInvoker.model.elements[0].type).isEqualTo(IRGitModel.Type.NonUnite.KeepCommit.Pick)
        assertThat(rebaseInvoker.model.elements[0].entry).isEqualTo(GitRebaseEntryGeneratedUsingLog(commit5.commit))

        assertThat(rebaseInvoker.model.elements[1].type).isEqualTo(IRGitModel.Type.NonUnite.KeepCommit.Pick)
        assertThat(rebaseInvoker.model.elements[1].entry).isEqualTo(GitRebaseEntryGeneratedUsingLog(commit4.commit))

        assertThat(rebaseInvoker.model.elements[2].type)
                .isInstanceOf(IRGitModel.Type.NonUnite.KeepCommit.Reword::class.java)
        assertThat(rebaseInvoker.model.elements[2].entry).isEqualTo(GitRebaseEntryGeneratedUsingLog(commit3.commit))

        assertThat(rebaseInvoker.model.elements[3].type).isEqualTo(IRGitModel.Type.NonUnite.Drop)
        assertThat(rebaseInvoker.model.elements[3].entry).isEqualTo(GitRebaseEntryGeneratedUsingLog(commit2.commit))

        assertThat(rebaseInvoker.model.elements[4].type).isEqualTo(IRGitModel.Type.NonUnite.KeepCommit.Pick)
        assertThat(rebaseInvoker.model.elements[4].entry).isEqualTo(GitRebaseEntryGeneratedUsingLog(commit1.commit))

    }

    fun testExecuteCommandsWithRebase(){
        rebaseInvoker.branchInfo = branchInfo
        val message = "commit2"
        rebaseInvoker.commands = mutableListOf(RebaseCommand(commit6),
                DropCommand(commit2), RewordCommand(commit3, message))
        rebaseInvoker.executeCommands()
        assertThat(rebaseInvoker.model.elements[0].type).isEqualTo(IRGitModel.Type.NonUnite.KeepCommit.Pick)
        assertThat(rebaseInvoker.model.elements[0].entry).isEqualTo(GitRebaseEntryGeneratedUsingLog(commit5.commit))

        assertThat(rebaseInvoker.model.elements[1].type).isEqualTo(IRGitModel.Type.NonUnite.KeepCommit.Pick)
        assertThat(rebaseInvoker.model.elements[1].entry).isEqualTo(GitRebaseEntryGeneratedUsingLog(commit4.commit))

        assertThat(rebaseInvoker.model.elements[2].type)
                .isInstanceOf(IRGitModel.Type.NonUnite.KeepCommit.Reword::class.java)
        assertThat(rebaseInvoker.model.elements[2].entry).isEqualTo(GitRebaseEntryGeneratedUsingLog(commit3.commit))

        assertThat(rebaseInvoker.model.elements[3].type).isEqualTo(IRGitModel.Type.NonUnite.Drop)
        assertThat(rebaseInvoker.model.elements[3].entry).isEqualTo(GitRebaseEntryGeneratedUsingLog(commit2.commit))

        assertThat(rebaseInvoker.model.elements[4].type).isEqualTo(IRGitModel.Type.NonUnite.KeepCommit.Pick)
        assertThat(rebaseInvoker.model.elements[4].entry).isEqualTo(GitRebaseEntryGeneratedUsingLog(commit1.commit))

    }




    fun testExecuteCherry(){
        assertTrue(rebaseInvoker.undoneCommands.isEmpty())
        assertTrue(rebaseInvoker.commitsToDisplayDuringRebase.isEmpty())
        rebaseInvoker.branchInfo = branchInfo
        rebaseInvoker.executeCherry()
        verify(rebaseInvoker.gitUtils).cherryPick(anyCustom())
    }

    fun testExpandCollapsedCommits(){
        rebaseInvoker.branchInfo = branchInfo
        rebaseInvoker.branchInfo.currentCommits = mutableListOf(commit3, commit4,commit5,commit6)
        val command = CollapseCommand(commit3,  mutableListOf(commit1,commit2))
        commit1.addChange(command)
        commit2.addChange(command)
        commit3.addChange(command)
        rebaseInvoker.expandCollapsedCommits()
        assertThat(rebaseInvoker.branchInfo.currentCommits.size).isEqualTo(6)

    }

    fun testExpandCollapsedCommitsWithDrop(){
        rebaseInvoker.branchInfo = branchInfo
        rebaseInvoker.branchInfo.currentCommits = mutableListOf(commit3, commit4,commit5,commit6)
        val command = CollapseCommand(commit3,  mutableListOf(commit1,commit2))
        val dropCommand = DropCommand(commit3)
        commit1.addChange(command)
        commit2.addChange(command)
        commit3.addChange(command)
        commit3.addChange(dropCommand)
        rebaseInvoker.expandCollapsedCommits()
        assertThat(rebaseInvoker.branchInfo.currentCommits.size).isEqualTo(6)

    }

    fun testRemoveChangesAfterPick(){
        rebaseInvoker.branchInfo = branchInfo
        val message = "commit2"

        val rebaseCommand = RebaseCommand(commit6)
        val dropCommand = DropCommand(commit2)
        val pickCommand = PickCommand(commit2)
        val rewordCommand = RewordCommand(commit3, message)

        rebaseInvoker.commands = mutableListOf(rebaseCommand,
               dropCommand ,pickCommand,rewordCommand )

        commit6.addChange(rebaseCommand)
        commit2.addChange(dropCommand)
        commit2.addChange(pickCommand)
        commit3.addChange(rewordCommand)
        rebaseInvoker.removeChangesBeforePick()
        assertThat(rebaseInvoker.commands.size).isEqualTo(3)

    }



    private inline fun <reified T> anyCustom(): T = ArgumentMatchers.any(T::class.java)
}