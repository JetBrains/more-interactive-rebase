package com.jetbrains.interactiveRebase.service

import com.intellij.mock.MockVirtualFile
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.GraphInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.FixupCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.PickCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import com.jetbrains.interactiveRebase.utils.gitUtils.IRGitUtils
import git4idea.GitCommit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class ModelServiceTest : BasePlatformTestCase() {
    private lateinit var modelService: ModelService
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var commitService: CommitService

    private lateinit var commit1: CommitInfo
    private lateinit var commit2: CommitInfo

    private lateinit var testProvider: TestGitCommitProvider

    override fun setUp() {
        super.setUp()
        coroutineScope = CoroutineScope(StandardTestDispatcher())
        commitService = mock(CommitService::class.java)
        modelService = ModelService(project, coroutineScope, commitService)

        commit1 = CommitInfo(mock(GitCommit::class.java), project)
        commit2 = CommitInfo(mock(GitCommit::class.java), project)

        modelService.branchInfo.initialCommits = mutableListOf(commit1, commit2)
        modelService.branchInfo.currentCommits = mutableListOf(commit2)

        testProvider = TestGitCommitProvider(project)
    }

    fun testAddToSelectedCommitsFixupInvolved() {
        val command = FixupCommand(commit2, mutableListOf(commit1))
        commit2.changes.add(command)

        modelService.addToSelectedCommits(commit2, modelService.branchInfo)
        assertThat(modelService.branchInfo.selectedCommits).containsExactlyInAnyOrder(commit1, commit2)
    }

    fun testAddToSelectedCommitsSquashInvolved() {
        val command = SquashCommand(commit2, mutableListOf(commit1), "squash")
        commit2.changes.add(command)

        modelService.addToSelectedCommits(commit2, modelService.branchInfo)
        assertThat(modelService.branchInfo.selectedCommits).containsExactlyInAnyOrder(commit1, commit2)
    }

    fun testRemoveFromSelectedCommits() {
        val command = SquashCommand(commit2, mutableListOf(commit1), "squash")
        commit2.changes.add(command)

        modelService.addToSelectedCommits(commit2, modelService.branchInfo)
        assertThat(modelService.branchInfo.selectedCommits).containsExactlyInAnyOrder(commit1, commit2)
    }

    fun testSelectedCommitLogic() {
        val commit3 = CommitInfo(mock(GitCommit::class.java), project)
        val commit4 = CommitInfo(mock(GitCommit::class.java), project)
        modelService.branchInfo.initialCommits = mutableListOf(commit1, commit2, commit3, commit4)
        modelService.branchInfo.currentCommits = mutableListOf(commit3, commit4)

        val command = SquashCommand(commit3, mutableListOf(commit1, commit2), "squash")
        commit3.changes.add(command)

        modelService.addToSelectedCommits(commit3, modelService.branchInfo)
        assertThat(modelService.getHighestSelectedCommit()).isEqualTo(commit1)
        assertThat(modelService.getLowestSelectedCommit()).isEqualTo(commit3)

        commit1.isSquashed = true
        commit2.isSquashed = true
        assertThat(modelService.getLastSelectedCommit()).isEqualTo(commit3)
    }

    fun testMarkRebaseCommitAsPaused() {
        val c1 = testProvider.createCommit("c1")
        val c2 = testProvider.createCommit("c2")
        val c3 = testProvider.createCommit("c3")
        val c4 = testProvider.createCommit("c4")

        modelService.branchInfo.initialCommits =
            mutableListOf(
                CommitInfo(c1, project),
                CommitInfo(c2, project),
                CommitInfo(c3, project),
                CommitInfo(c4, project),
            )

        modelService.branchInfo.currentCommits = modelService.branchInfo.initialCommits.toMutableList()
        modelService.branchInfo.currentCommits[2].isPaused = true

        modelService.markRebaseCommitAsPaused("MockHash(string='c2')")
        assertThat(modelService.branchInfo.initialCommits[1].isPaused).isTrue()
        assertThat(modelService.branchInfo.initialCommits[0].isRebased).isFalse()
        assertThat(modelService.branchInfo.initialCommits[2].isRebased).isTrue()
        assertThat(modelService.branchInfo.initialCommits[3].isRebased).isTrue()
    }

    fun testRemoveAllChangesIfNeeded() {
        val commit3 = CommitInfo(mock(GitCommit::class.java), project)
        modelService.branchInfo.initialCommits = mutableListOf(commit1, commit2, commit3)
        modelService.branchInfo.currentCommits = mutableListOf(commit2, commit3)

        project.service<RebaseInvoker>().commands.add(PickCommand(commit2))
        project.service<RebaseInvoker>().undoneCommands.add(PickCommand(commit1))

        commit2.isSquashed = true
        commit1.isRebased = true
        commit1.isCollapsed = true
        commit2.isPaused = true

        commit2.changes.add(SquashCommand(commit2, mutableListOf(commit1), "squash"))
        commit1.changes.add(FixupCommand(commit1, mutableListOf(commit2)))

        modelService.removeAllChangesIfNeeded()
        assertThat(commit1.isRebased).isFalse()
        assertThat(commit1.isSquashed).isFalse()
        assertThat(commit1.isPaused).isFalse()
        assertThat(commit1.isCollapsed).isFalse()

        assertThat(commit2.isRebased).isFalse()
        assertThat(commit2.isSquashed).isFalse()
        assertThat(commit2.isPaused).isFalse()
        assertThat(commit2.isCollapsed).isFalse()

        assertThat(project.service<RebaseInvoker>().commands).isEmpty()
        assertThat(project.service<RebaseInvoker>().undoneCommands).isEmpty()
    }

    fun testRefreshDuringRebaseProcess() {
        val gitUtil = mock(IRGitUtils::class.java)
        val vf = MockVirtualFile("namey")
        modelService.gitUtils = gitUtil
        `when`(gitUtil.getCurrentRebaseCommit(project, vf)).thenReturn("MockHash(string='lala')")
        val c3 = testProvider.createCommit("lala")
        val commit3 = CommitInfo(c3, project)

        modelService.branchInfo.initialCommits = mutableListOf(commit1, commit3)
        modelService.branchInfo.currentCommits = mutableListOf(commit3)

        modelService.refreshModelDuringRebaseProcess(vf)
        assertThat(commit3.isPaused).isTrue()
        assertThat(modelService.rebaseInProcess).isTrue()
        assertThat(project.service<ActionService>().getHeaderPanel().rebaseProcessPanel.isVisible).isTrue()
        assertThat(project.service<ActionService>().getHeaderPanel().changeActionsPanel.isVisible).isFalse()
    }

    fun testRefreshAfterRebaseProcess() {
        val c1 = CommitInfo(testProvider.createCommit("c1"), project)
        val c2 = CommitInfo(testProvider.createCommit("c2"), project)
        val c3 = CommitInfo(testProvider.createCommit("c3"), project)
        val c4 = CommitInfo(testProvider.createCommit("c4"), project)

        modelService.branchInfo.initialCommits = mutableListOf(c1, c2, c3, c4)

        modelService.branchInfo.currentCommits = mutableListOf(c1, c2, c3, c4)
        c1.addChange(PickCommand(c1))
        c2.addChange(PickCommand(c2))
        c3.addChange(PickCommand(c3))
        c3.isRebased = true

        modelService.rebaseInProcess = true
        modelService.previousConflictCommit = "lol"
        modelService.refreshModel()
        assertThat(modelService.rebaseInProcess).isFalse()
        assertThat(project.service<ActionService>().getHeaderPanel().rebaseProcessPanel.isVisible).isFalse()
        assertThat(project.service<ActionService>().getHeaderPanel().changeActionsPanel.isVisible).isTrue()
        assertThat(c1.changes).isEmpty()
        assertThat(c2.changes).isEmpty()
        assertThat(c3.changes).isEmpty()
        assertThat(c3.isRebased).isFalse()
        assertThat(modelService.previousConflictCommit).isEqualTo("")
    }

    fun testGraphDuplicatesBothBranches() {
        val primaryBranch = BranchInfo("primary", listOf(commit1))
        commit2.isCollapsed = true
        val secondaryBranch = BranchInfo("secondary", listOf(commit2))
        val graph = GraphInfo(primaryBranch, secondaryBranch)

        val copy = modelService.duplicateGraphInfo(graph)
        assertThat(copy === graph).isFalse()
        assertThat(copy.mainBranch === primaryBranch).isFalse()
        assertThat(copy.addedBranch).isNotNull()
        assertThat(copy.addedBranch === graph.addedBranch).isFalse()

        val copyCommit = copy.mainBranch.currentCommits[0]
        copyCommit.isSelected = true
        copyCommit.changes.add(DropCommand(copyCommit))

        assertThat(commit1.changes.isEmpty()).isTrue()

        val copyAddedCommit: CommitInfo = copy.addedBranch!!.currentCommits[0]
        assertThat(copyAddedCommit.isCollapsed).isTrue()
    }

    fun testGraphDuplicateNullBase() {
        val primaryBranch = BranchInfo("primary", listOf(commit1))
        commit2.isCollapsed = true
        val secondaryBranch = BranchInfo("secondary", listOf(commit2))
        secondaryBranch.baseCommit = null
        val graph = GraphInfo(primaryBranch, secondaryBranch)

        val copy = modelService.duplicateGraphInfo(graph)
        assertThat(copy === graph).isFalse()
        assertThat(copy.mainBranch === primaryBranch).isFalse()
        assertThat(copy.addedBranch).isNotNull()
        assertThat(copy.addedBranch === graph.addedBranch).isFalse()

        val copyCommit = copy.mainBranch.currentCommits[0]
        copyCommit.isSelected = true
        copyCommit.changes.add(DropCommand(copyCommit))

        assertThat(commit1.changes.isEmpty()).isTrue()

        val copyAddedCommit: CommitInfo = copy.addedBranch!!.currentCommits[0]
        assertThat(copyAddedCommit.isCollapsed).isTrue()
    }

    fun testGraphDuplicatesOneBranch() {
        val primaryBranch = BranchInfo("primary", listOf(commit1))
        commit2.isCollapsed = true
        val graph = GraphInfo(primaryBranch)

        val copy = modelService.duplicateGraphInfo(graph)
        assertThat(copy === graph).isFalse()
        assertThat(copy.mainBranch === primaryBranch).isFalse()
        assertThat(copy.addedBranch).isNull()
    }
}
