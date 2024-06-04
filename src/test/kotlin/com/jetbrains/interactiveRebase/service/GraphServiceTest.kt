package com.jetbrains.interactiveRebase.service

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.GraphInfo
import com.jetbrains.interactiveRebase.exceptions.IRInaccessibleException
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.services.GraphService
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.mockito.Mockito.any
import org.mockito.Mockito.anyList
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class GraphServiceTest : BasePlatformTestCase() {
    private lateinit var graphService: GraphService
    private lateinit var commitService: CommitService
    private lateinit var commit1: CommitInfo
    private lateinit var provider: TestGitCommitProvider

    override fun setUp() {
        super.setUp()
        this.commitService = mock(CommitService::class.java)
        this.graphService = GraphService(project, commitService)
        this.provider = TestGitCommitProvider(project)
        this.commit1 = CommitInfo(provider.createCommit("add tests"), project)
    }

    fun testGetBranchingCommit() {
        val commit = CommitInfo(provider.createCommitWithParent("fix test", "parent"), project)
        val commitParent = CommitInfo(provider.createCommit("before tests"), project)
        `when`(commitService.turnHashToCommit(anyString())).thenReturn(commitParent.commit)
        `when`(commitService.getCommitInfoForBranch(anyCustom())).thenReturn(listOf(commitParent))

        val res = graphService.getBranchingCommit(commit)
        verify(commitService).turnHashToCommit("parent")
        verify(commitService).getCommitInfoForBranch(listOf(commitParent.commit))
        assertThat(res).isEqualTo(commitParent)
    }

    fun testGetBranchingCommitChecksEmpty() {
        assertThatThrownBy { graphService.getBranchingCommit(commit1) }
            .isInstanceOf(IRInaccessibleException::class.java)
            .withFailMessage("Branching-off commit cannot be displayed")
    }

    fun testGetBranchingCommitChecksMergeCommit() {
        val commit = CommitInfo(provider.createCommitWithParent("fix test", "mom", "dad"), project)
        assertThatThrownBy { graphService.getBranchingCommit(commit) }
            .isInstanceOf(IRInaccessibleException::class.java)
            .withFailMessage("Branching-off commit cannot be displayed")
    }

    fun testUpdateBranchInfoInitName() {
        val branchInfo = BranchInfo("")
        `when`(commitService.getBranchName()).thenReturn("current")
        graphService.updateBranchInfo(branchInfo)
        assertThat(project.service<RebaseInvoker>().branchInfo.name).isEqualTo("current")
    }

    fun testUpdateBranchInfoNoChange() {
        val branchInfo = BranchInfo("feature1")
        graphService.updateBranchInfo(branchInfo)
        assertThat(project.service<RebaseInvoker>().branchInfo).isNotEqualTo(branchInfo)
    }

    fun testUpdateBranchInfoSetsAllChanges() {
        val branchInfo = BranchInfo("")
        `when`(commitService.getBranchName()).thenReturn("feature1")
        `when`(commitService.getCommits("feature1")).thenReturn(listOf(commit1.commit))
        `when`(commitService.getCommitInfoForBranch(anyList())).thenReturn(listOf(commit1))
        branchInfo.selectedCommits.add(commit1)
        graphService.updateBranchInfo(branchInfo)
        val resultBranch = project.service<RebaseInvoker>().branchInfo
        assertThat(resultBranch.selectedCommits).isEmpty()
        assertThat(resultBranch.name).isEqualTo("feature1")
        assertThat(resultBranch.initialCommits).isEqualTo(listOf(commit1))
    }

    fun testUpdateBranchInfoSetsOnlyCommitChanges() {
        val branchInfo = BranchInfo("feature1")
        `when`(commitService.getBranchName()).thenReturn("feature1")
        `when`(commitService.getCommits("feature1")).thenReturn(listOf(commit1.commit))
        `when`(commitService.getCommitInfoForBranch(anyList())).thenReturn(listOf(commit1))
        branchInfo.selectedCommits.add(commit1)
        graphService.updateBranchInfo(branchInfo)
        val resultBranch = project.service<RebaseInvoker>().branchInfo
        assertThat(resultBranch.selectedCommits).isEmpty()
        assertThat(resultBranch.name).isEqualTo("feature1")
        assertThat(resultBranch.initialCommits).isEqualTo(listOf(commit1))
    }

    fun testUpdateBranchInfoIncludeBranching() {
        val branchInfo = BranchInfo("feature1")
        val commit = CommitInfo(provider.createCommitWithParent("with parent", "parent"), project)
        `when`(commitService.getBranchName()).thenReturn("feature1")
        `when`(commitService.getCommits("feature1")).thenReturn(listOf(commit.commit))
        `when`(commitService.getCommitInfoForBranch(anyList())).thenReturn(listOf(commit1))
        `when`(commitService.getCommitInfoForBranch(listOf(commit.commit))).thenReturn(listOf(commit))
        `when`(commitService.turnHashToCommit("parent")).thenReturn(commit1.commit)
        graphService.updateBranchInfo(branchInfo, true)
        val resultBranch = project.service<RebaseInvoker>().branchInfo
        assertThat(resultBranch.selectedCommits).isEmpty()
        assertThat(resultBranch.name).isEqualTo("feature1")
        assertThat(resultBranch.initialCommits).hasSameElementsAs(listOf(commit1, commit))
    }

    fun testAddBranchOneInEach() {
        val checkedOut = BranchInfo("")
        val startingCommit1 = CommitInfo(provider.createCommitWithParent("with parent in f1", "parent"), project)
        val startingCommit2 = CommitInfo(provider.createCommitWithParent("with parent in f2", "parent"), project)
        val parentCommit = CommitInfo(provider.createCommit("parent"), project)
        val graphInfo = GraphInfo(checkedOut)
        `when`(commitService.getCommits("feature2")).thenReturn(listOf(startingCommit2.commit))
        `when`(commitService.getCommits("feature1")).thenReturn(listOf(startingCommit1.commit))
        `when`(commitService.getBranchName()).thenReturn("feature1")
        `when`(commitService.getCommitInfoForBranch(listOf(startingCommit1.commit))).thenReturn(listOf(startingCommit1))
        `when`(commitService.getCommitInfoForBranch(listOf(parentCommit.commit))).thenReturn(listOf(parentCommit))
        `when`(commitService.getCommitInfoForBranch(listOf(startingCommit2.commit))).thenReturn(listOf(startingCommit2))
        `when`(commitService.turnHashToCommit("parent")).thenReturn(parentCommit.commit)

        graphService.addBranch(graphInfo, "feature2")
        val expAdded = BranchInfo("feature2", listOf(startingCommit2, parentCommit))
        assertThat(graphInfo.addedBranch).isEqualTo(expAdded)
        assertThat(checkedOut.name).isEqualTo("feature1")
        assertThat(checkedOut.currentCommits).doesNotContain(parentCommit)
    }

    fun testUpgradeGraphInfoOneBranch() {
        val branch = BranchInfo("")
        val graphInfo = GraphInfo(branch)
        `when`(commitService.getCommits("feature1")).thenReturn(listOf(commit1.commit))
        `when`(commitService.getBranchName()).thenReturn("feature1")
        `when`(commitService.getCommitInfoForBranch(listOf(commit1.commit))).thenReturn(listOf(commit1))
        graphService.updateGraphInfo(graphInfo)
        assertThat(branch.name).isEqualTo("feature1")
        assertThat(branch.initialCommits).isEqualTo(listOf(commit1))
        assertThat(graphInfo.addedBranch).isNull()
    }

    fun testUpgradeGraphInfoTwoBranches() {
        val checkedOut = BranchInfo("")
        val startingCommit1 = CommitInfo(provider.createCommitWithParent("with parent in f1", "parent"), project)
        val startingCommit2 = CommitInfo(provider.createCommitWithParent("with parent in f2", "parent"), project)
        val addedCommit = CommitInfo(provider.createCommitWithParent("on top added", "with parent in f2"), project)
        val parentCommit = CommitInfo(provider.createCommit("parent"), project)
        val added = BranchInfo("feature2", listOf(startingCommit1))
        val graphInfo = GraphInfo(checkedOut, added)
        `when`(commitService.getCommits("feature2")).thenReturn(listOf(addedCommit.commit, startingCommit2.commit))
        `when`(commitService.getCommits("feature1")).thenReturn(listOf(startingCommit1.commit))
        `when`(commitService.getBranchName()).thenReturn("feature1")
        `when`(commitService.getCommitInfoForBranch(listOf(startingCommit1.commit))).thenReturn(listOf(startingCommit1))
        `when`(commitService.getCommitInfoForBranch(listOf(parentCommit.commit))).thenReturn(listOf(parentCommit))
        `when`(
            commitService.getCommitInfoForBranch(listOf(addedCommit.commit, startingCommit2.commit)),
        ).thenReturn(listOf(addedCommit, startingCommit2))
        `when`(commitService.turnHashToCommit("parent")).thenReturn(parentCommit.commit)
        graphService.updateGraphInfo(graphInfo)
        assertThat(checkedOut.name).isEqualTo("feature1")
        assertThat(checkedOut.initialCommits).isEqualTo(listOf(startingCommit1))
        assertThat(graphInfo.addedBranch?.currentCommits).contains(addedCommit)
        assertThat(graphInfo.addedBranch?.currentCommits).contains(startingCommit2)
    }

    private inline fun <reified T> anyCustom(): T = any(T::class.java)
}
