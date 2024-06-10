package com.jetbrains.interactiveRebase.service

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.FixupCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.services.ModelService
import git4idea.GitCommit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.StandardTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.mock

class ModelServiceTest : BasePlatformTestCase() {
    private lateinit var modelService: ModelService
    private lateinit var coroutineScope: CoroutineScope
    private lateinit var commitService: CommitService

    private lateinit var commit1: CommitInfo
    private lateinit var commit2: CommitInfo

    init {
        System.setProperty("idea.home.path", "/tmp")
    }

    override fun setUp() {
        super.setUp()
        coroutineScope = CoroutineScope(StandardTestDispatcher())
        commitService = mock(CommitService::class.java)
        modelService = ModelService(project, coroutineScope, commitService)

        commit1 = CommitInfo(mock(GitCommit::class.java), project)
        commit2 = CommitInfo(mock(GitCommit::class.java), project)

        modelService.branchInfo.initialCommits = mutableListOf(commit1, commit2)
        modelService.branchInfo.currentCommits = mutableListOf(commit2)
    }

    fun testAddToSelectedCommitsFixupInvolved() {
        val command = FixupCommand(commit2, mutableListOf(commit1))
        commit2.changes.add(command)

        modelService.addToSelectedCommits(commit2)
        assertThat(modelService.branchInfo.selectedCommits).containsExactlyInAnyOrder(commit1, commit2)
    }

    fun testAddToSelectedCommitsSquashInvolved() {
        val command = SquashCommand(commit2, mutableListOf(commit1), "squash")
        commit2.changes.add(command)

        modelService.addToSelectedCommits(commit2)
        assertThat(modelService.branchInfo.selectedCommits).containsExactlyInAnyOrder(commit1, commit2)
    }

    fun testRemoveFromSelectedCommits() {
        val command = SquashCommand(commit2, mutableListOf(commit1), "squash")
        commit2.changes.add(command)

        modelService.addToSelectedCommits(commit2)
        assertThat(modelService.branchInfo.selectedCommits).containsExactlyInAnyOrder(commit1, commit2)
    }

    fun testSelectedCommitLogic() {
        val commit3 = CommitInfo(mock(GitCommit::class.java), project)
        val commit4 = CommitInfo(mock(GitCommit::class.java), project)
        modelService.branchInfo.initialCommits = mutableListOf(commit1, commit2, commit3, commit4)
        modelService.branchInfo.currentCommits = mutableListOf(commit3, commit4)

        val command = SquashCommand(commit3, mutableListOf(commit1, commit2), "squash")
        commit3.changes.add(command)

        modelService.addToSelectedCommits(commit3)
        assertThat(modelService.getHighestSelectedCommit()).isEqualTo(commit1)
        assertThat(modelService.getLowestSelectedCommit()).isEqualTo(commit3)

        commit1.isSquashed = true
        commit2.isSquashed = true
        assertThat(modelService.getLastSelectedCommit()).isEqualTo(commit3)
    }
}
