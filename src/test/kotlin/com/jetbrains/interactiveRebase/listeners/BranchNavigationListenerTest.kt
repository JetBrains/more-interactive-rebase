package com.jetbrains.interactiveRebase.listeners

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.services.ModelService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class BranchNavigationListenerTest : BasePlatformTestCase() {
    init {
        System.setProperty("idea.home.path", "/tmp")
    }

    private lateinit var listener: BranchNavigationListener
    private lateinit var modelService: ModelService
    private lateinit var commit1: CommitInfo
    private lateinit var commit2: CommitInfo
    private lateinit var commit3: CommitInfo
    private lateinit var commit4: CommitInfo
    private lateinit var branchInfo: BranchInfo

    override fun setUp() {
        super.setUp()

        val commitProvider = TestGitCommitProvider(project)
        commit1 = CommitInfo(commitProvider.createCommit("commit1"), project)
        commit2 = CommitInfo(commitProvider.createCommit("commit2"), project)
        commit3 = CommitInfo(commitProvider.createCommit("commit3"), project)
        commit4 = CommitInfo(commitProvider.createCommit("commit4"), project)

        val commitService = mock(CommitService::class.java)

        `when`(commitService.getCommits("my branch")).thenReturn(listOf(commit4.commit, commit3.commit, commit2.commit, commit1.commit))

        `when`(commitService.getBranchName()).thenReturn("my branch")

        modelService = ModelService(project, CoroutineScope(Dispatchers.Default), commitService)

        listener = BranchNavigationListener(project, modelService)
        branchInfo = modelService.branchInfo
    }

    fun testUpNoCommitsSelected() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))
        listener.up()

        assertThat(branchInfo.selectedCommits.size).isEqualTo(1)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit1)
    }

    fun testDownNoCommitsSelected() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))
        listener.down()

        assertThat(branchInfo.selectedCommits.size).isEqualTo(1)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit4)
    }

    fun testUpOutOfBounds() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))
        modelService.selectSingleCommit(commit4, branchInfo)
        listener.up()

        assertThat(branchInfo.selectedCommits.size).isEqualTo(1)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit4)
    }

    fun testUpFirstCommitSelected() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))
        modelService.selectSingleCommit(commit1, branchInfo)
        listener.up()

        assertThat(branchInfo.selectedCommits.size).isEqualTo(1)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit2)
    }

    fun testUpFirstCommitSelectedCollapsedCase() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))
        modelService.selectSingleCommit(commit1, branchInfo)
        commit2.isCollapsed = true
        listener.up()

        assertThat(branchInfo.selectedCommits.size).isEqualTo(1)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit3)
    }

    fun testDownLastCommitSelected() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))
        modelService.selectSingleCommit(commit4, branchInfo)
        listener.down()

        assertThat(branchInfo.selectedCommits.size).isEqualTo(1)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit3)
    }

    fun testDownCollapsedCase() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))
        modelService.selectSingleCommit(commit4, branchInfo)
        commit3.isCollapsed = true
        listener.down()

        assertThat(branchInfo.selectedCommits.size).isEqualTo(1)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit2)
    }

    fun testDownOutOfBounds() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))
        modelService.selectSingleCommit(commit1, branchInfo)
        listener.down()

        assertThat(branchInfo.selectedCommits.size).isEqualTo(1)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit1)
    }
}
