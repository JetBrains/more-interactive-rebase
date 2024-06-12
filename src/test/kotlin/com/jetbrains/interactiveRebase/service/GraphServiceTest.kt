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
    private lateinit var commit2: CommitInfo
    private lateinit var commitParent: CommitInfo
    private lateinit var addedCommit1: CommitInfo

    init {
        System.setProperty("idea.home.path", "/tmp")
    }

    override fun setUp() {
        super.setUp()
        this.provider = TestGitCommitProvider(project)
        this.commit1 = CommitInfo(provider.createCommitWithParent("fix test", "before tests"), project)
        this.commit2 = CommitInfo(provider.createCommitWithParent("add something", "fix test"), project)
        this.commitParent = CommitInfo(provider.createCommit("before tests"), project)
        this.addedCommit1 = CommitInfo(provider.createCommitWithParent("in reference", "before tests"), project)

        this.commitService = mock(CommitService::class.java)
        this.graphService = GraphService(project, commitService)

//        this.commit1 = CommitInfo(provider.createCommit("add tests"), project)
    }

    fun testGetBranchingCommit() {
        val b1 = BranchInfo("feature", initialCommits = listOf(commit2, commit1))
        val b2 = BranchInfo("dev", initialCommits = listOf(addedCommit1))

        val graph = GraphInfo(b1, b2)
        `when`(commitService.turnHashToCommit(anyString())).thenReturn(commitParent.commit)
        `when`(commitService.getCommitInfoForBranch(anyCustom())).thenReturn(listOf(commitParent))

        val res = graphService.getBranchingCommit(graph)
        verify(commitService).turnHashToCommit(commitParent.commit.id.asString())
        verify(commitService).getCommitInfoForBranch(listOf(commitParent.commit))
        assertThat(res).isEqualTo(commitParent)
    }

    fun testGetBranchingCommitChecksEmpty() {
        val b1 = BranchInfo("feature")
        val b2 = BranchInfo("dev", initialCommits = listOf(addedCommit1))

        val graph = GraphInfo(b1, b2)
        assertThatThrownBy { graphService.getBranchingCommit(graph) }
            .isInstanceOf(IRInaccessibleException::class.java)
            .withFailMessage("Branching-off commit cannot be displayed. Cannot find the added branch or the commits on the primary branch")
    }

    fun testGetBranchingCommitChecksNullAdded() {
        val b1 = BranchInfo("feature", initialCommits = listOf(commit1))

        val graph = GraphInfo(b1, null)
        assertThatThrownBy { graphService.getBranchingCommit(graph) }
            .isInstanceOf(IRInaccessibleException::class.java)
            .withFailMessage("Branching-off commit cannot be displayed. Cannot find the added branch or the commits on the primary branch")
    }

    fun testGetBranchingCommitChecksNoParent() {
        val b1 = BranchInfo("feature", initialCommits = listOf(commitParent))
        val b2 = BranchInfo("dev", initialCommits = listOf(addedCommit1))

        val graph = GraphInfo(b1, b2)
        assertThatThrownBy { graphService.getBranchingCommit(graph) }
            .isInstanceOf(IRInaccessibleException::class.java)
            .withFailMessage("Trying to display parents of initial commit")
    }

//
    fun testGetBranchingCommitChecksMergeCommit() {
        val mergeCommit =
            CommitInfo(
                provider.createCommitWithParent("fix test", "other parent", commitParent.commit.id.asString()),
                project,
            )

        val b1 = BranchInfo("feature", initialCommits = listOf(mergeCommit))
        val b2 = BranchInfo("dev", initialCommits = listOf(addedCommit1))
        `when`(commitService.turnHashToCommit(anyString())).thenReturn(commitParent.commit)
        `when`(commitService.getCommitInfoForBranch(anyCustom())).thenReturn(listOf(commitParent))

        val graph = GraphInfo(b1, b2)
        val res = graphService.getBranchingCommit(graph)
        verify(commitService).turnHashToCommit(commitParent.commit.id.asString())
        verify(commitService).getCommitInfoForBranch(listOf(commitParent.commit))
        assertThat(res).isEqualTo(commitParent)
    }

    fun testGetBranchingCommitChecksMergeCommitNoIntersection() {
        val mergeCommit =
            CommitInfo(
                provider.createCommitWithParent("fix test", commitParent.commit.id.asString(), "other parent"),
                project,
            )

        val b1 = BranchInfo("feature", initialCommits = listOf(mergeCommit))
        val b2 = BranchInfo("dev", initialCommits = listOf(commit2))
        `when`(commitService.turnHashToCommit(anyString())).thenReturn(commitParent.commit)
        `when`(commitService.getCommitInfoForBranch(anyCustom())).thenReturn(listOf(commitParent))

        val graph = GraphInfo(b1, b2)
        val res = graphService.getBranchingCommit(graph)
        verify(commitService).turnHashToCommit(commitParent.commit.id.asString())
        verify(commitService).getCommitInfoForBranch(listOf(commitParent.commit))
        assertThat(res).isEqualTo(commitParent)
    }

    fun testUpdateBranchInfoInitName() {
        val branchInfo = BranchInfo("")
        `when`(commitService.getBranchName()).thenReturn("current")
        graphService.updateBranchInfo(branchInfo)
        assertThat(project.service<RebaseInvoker>().branchInfo.name).isEqualTo("current")
    }

    fun testUpdateBranchInfoNoChange() {
        val branchInfo = BranchInfo("feature1")
        `when`(commitService.getBranchName()).thenReturn("feature1")
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

    fun testAddBranchOneInEach() {
        val checkedOut = BranchInfo("")
        val startingCommit1 = CommitInfo(provider.createCommitWithParent("with parent in f1", "parent"), project)
        val startingCommit2 = CommitInfo(provider.createCommitWithParent("with parent in f2", "parent"), project)
        val parentCommit = CommitInfo(provider.createCommit("parent"), project)
        val graphInfo = GraphInfo(checkedOut)
        `when`(commitService.getCommitsWithReference("feature2", "feature1")).thenReturn(listOf(startingCommit2.commit))
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

    fun testAddBranchChecksEmptyPrimary() {
        val checkedOut = BranchInfo("")
        val startingCommit1 = CommitInfo(provider.createCommitWithParent("with parent in f1", "parent"), project)
        val startingCommit2 = CommitInfo(provider.createCommitWithParent("with parent in f2", "parent"), project)
        val parentCommit = CommitInfo(provider.createCommit("parent"), project)
        val graphInfo = GraphInfo(checkedOut)
        `when`(commitService.getCommitsWithReference("feature2", "feature1")).thenReturn(listOf(startingCommit2.commit))
        `when`(commitService.getCommits("feature1")).thenReturn(listOf())
        `when`(commitService.getBranchName()).thenReturn("feature1")
        `when`(commitService.getCommitInfoForBranch(listOf(startingCommit1.commit))).thenReturn(listOf(startingCommit1))
        `when`(commitService.getCommitInfoForBranch(listOf(parentCommit.commit))).thenReturn(listOf(parentCommit))
        `when`(commitService.getCommitInfoForBranch(listOf(startingCommit2.commit))).thenReturn(listOf(startingCommit2))
        `when`(commitService.turnHashToCommit("parent")).thenReturn(parentCommit.commit)

        graphService.addBranch(graphInfo, "feature2")
        assertThat(graphInfo.addedBranch).isNull()
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
        `when`(commitService.getCommitsWithReference("feature2", "feature1")).thenReturn(listOf(addedCommit.commit, startingCommit2.commit))
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

    fun testRemoveBranch() {
        val b1 = BranchInfo("feature", initialCommits = listOf(commit2, commit1))
        val b2 = BranchInfo("dev", initialCommits = listOf(addedCommit1))

        `when`(commitService.getBranchName()).thenReturn("feature1")
        `when`(commitService.turnHashToCommit(anyString())).thenReturn(commitParent.commit)
        `when`(commitService.getCommitInfoForBranch(anyCustom())).thenReturn(listOf(commitParent))
        `when`(commitService.getCommits("feature")).thenReturn(listOf(commit2.commit, commit1.commit))

        val graph = GraphInfo(b1, b2)
        graphService.removeBranch(graph)
        assertThat(graph.addedBranch).isNull()
        assertThat(graph.mainBranch.isPrimary).isFalse()
    }

    private inline fun <reified T> anyCustom(): T = any(T::class.java)
}
