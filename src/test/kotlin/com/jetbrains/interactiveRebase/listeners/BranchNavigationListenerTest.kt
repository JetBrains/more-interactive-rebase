package com.jetbrains.interactiveRebase.listeners

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.visuals.MainPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.awt.event.KeyEvent

class BranchNavigationListenerTest : BasePlatformTestCase() {
    private lateinit var listener: BranchNavigationListener
    private lateinit var modelService: ModelService
    private lateinit var commit1: CommitInfo
    private lateinit var commit2: CommitInfo
    private lateinit var commit3: CommitInfo
    private lateinit var commit4: CommitInfo
    private lateinit var branchInfo: BranchInfo
    private lateinit var mainPanel: MainPanel
    private lateinit var upEvent: KeyEvent
    private lateinit var downEvent: KeyEvent
    private lateinit var shiftUpEvent: KeyEvent
    private lateinit var shiftDownEvent: KeyEvent
    private lateinit var altUpEvent: KeyEvent
    private lateinit var altDownEvent: KeyEvent

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

        mainPanel = MainPanel(project)

        upEvent = KeyEvent(mainPanel, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_UP, KeyEvent.CHAR_UNDEFINED)
        downEvent = KeyEvent(mainPanel, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_DOWN, KeyEvent.CHAR_UNDEFINED)
        shiftUpEvent =
            KeyEvent(
                mainPanel,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                KeyEvent.SHIFT_DOWN_MASK,
                KeyEvent.VK_UP,
                KeyEvent.CHAR_UNDEFINED,
            )
        shiftDownEvent =
            KeyEvent(
                mainPanel,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                KeyEvent.SHIFT_DOWN_MASK,
                KeyEvent.VK_DOWN,
                KeyEvent.CHAR_UNDEFINED,
            )
        altUpEvent =
            KeyEvent(
                mainPanel,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                KeyEvent.ALT_DOWN_MASK,
                KeyEvent.VK_UP,
                KeyEvent.CHAR_UNDEFINED,
            )
        altDownEvent =
            KeyEvent(
                mainPanel,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                KeyEvent.ALT_DOWN_MASK,
                KeyEvent.VK_DOWN,
                KeyEvent.CHAR_UNDEFINED,
            )
    }

    fun testUpNoCommitsSelected() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))
        listener.keyPressed(upEvent)

        assertThat(branchInfo.selectedCommits.size).isEqualTo(1)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit1)
    }

    fun testDownNoCommitsSelected() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))
        listener.keyPressed(downEvent)

        assertThat(branchInfo.selectedCommits.size).isEqualTo(1)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit4)
    }

    fun testUpOutOfBounds() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))
        modelService.selectSingleCommit(commit4, branchInfo)
        listener.keyPressed(upEvent)

        assertThat(branchInfo.selectedCommits.size).isEqualTo(1)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit4)
    }

    fun testUpFirstCommitSelected() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))
        modelService.selectSingleCommit(commit1, branchInfo)
        listener.keyPressed(upEvent)

        assertThat(branchInfo.selectedCommits.size).isEqualTo(1)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit2)
    }

    fun testUpFirstCommitSelectedCollapsedCase() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))
        modelService.selectSingleCommit(commit1, branchInfo)
        commit2.isCollapsed = true
        listener.keyPressed(upEvent)

        assertThat(branchInfo.selectedCommits.size).isEqualTo(1)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit3)
    }

    fun testDownLastCommitSelected() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))
        modelService.selectSingleCommit(commit4, branchInfo)
        listener.keyPressed(downEvent)

        assertThat(branchInfo.selectedCommits.size).isEqualTo(1)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit3)
    }

    fun testDownCollapsedCase() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))
        modelService.selectSingleCommit(commit4, branchInfo)
        commit3.isCollapsed = true
        listener.keyPressed(downEvent)

        assertThat(branchInfo.selectedCommits.size).isEqualTo(1)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit2)
    }

    fun testDownOutOfBounds() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))
        modelService.selectSingleCommit(commit1, branchInfo)
        listener.keyPressed(downEvent)

        assertThat(branchInfo.selectedCommits.size).isEqualTo(1)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit1)
    }

    fun testShiftUp() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))

        listener.keyPressed(upEvent)
        listener.keyPressed(shiftUpEvent)

        assertThat(branchInfo.selectedCommits.size).isEqualTo(2)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit1)
        assertThat(branchInfo.selectedCommits[1]).isEqualTo(commit2)
    }

    fun testShiftDown() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))

        listener.keyPressed(downEvent)
        listener.keyPressed(shiftDownEvent)

        assertThat(branchInfo.selectedCommits.size).isEqualTo(2)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit4)
        assertThat(branchInfo.selectedCommits[1]).isEqualTo(commit3)
    }

    fun testAltUp() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))

        listener.keyPressed(upEvent)
        listener.keyPressed(altUpEvent)

        assertThat(branchInfo.selectedCommits.size).isEqualTo(1)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit1)
        assertThat(branchInfo.currentCommits[2]).isEqualTo(commit1)
        assertThat(branchInfo.currentCommits[3]).isEqualTo(commit2)
    }

    fun testAltDown() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))

        listener.keyPressed(downEvent)
        listener.keyPressed(altDownEvent)

        assertThat(branchInfo.selectedCommits.size).isEqualTo(1)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit4)
        assertThat(branchInfo.currentCommits[1]).isEqualTo(commit4)
        assertThat(branchInfo.currentCommits[0]).isEqualTo(commit3)
    }
}
