package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.visuals.MainPanel
import com.jetbrains.interactiveRebase.visuals.multipleBranches.SideBranchPanel
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
    private lateinit var rightEvent: KeyEvent
    private lateinit var leftEvent: KeyEvent

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
        project.service<ActionService>().mainPanel = mainPanel

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

        rightEvent =
            KeyEvent(
                mainPanel,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                KeyEvent.SHIFT_DOWN_MASK,
                KeyEvent.VK_RIGHT,
                KeyEvent.CHAR_UNDEFINED,
            )

        leftEvent =
            KeyEvent(
                mainPanel,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                KeyEvent.SHIFT_DOWN_MASK,
                KeyEvent.VK_LEFT,
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

    fun testShiftUpNothingSelected() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))

        listener.keyPressed(shiftUpEvent)

        assertThat(branchInfo.selectedCommits.size).isEqualTo(1)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit1)
    }

    fun testShiftUpCaps() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))

        listener.keyPressed(shiftUpEvent)
        listener.keyPressed(shiftUpEvent)
        listener.keyPressed(shiftUpEvent)
        listener.keyPressed(shiftUpEvent)
        listener.keyPressed(shiftUpEvent)

        assertThat(branchInfo.selectedCommits.size).isEqualTo(4)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit1)
        assertThat(branchInfo.selectedCommits[1]).isEqualTo(commit2)
        assertThat(branchInfo.selectedCommits[2]).isEqualTo(commit3)
        assertThat(branchInfo.selectedCommits[3]).isEqualTo(commit4)
    }

    fun testShiftUpCollapsed() {
        branchInfo.clearSelectedCommits()
        var provider = TestGitCommitProvider(project)
        var commit5 = CommitInfo(provider.createCommit("commit5"), project)
        var commit6 = CommitInfo(provider.createCommit("commit6"), project)
        var commit7 = CommitInfo(provider.createCommit("commit7"), project)
        var commit8 = CommitInfo(provider.createCommit("commit5"), project)
        var commit9 = CommitInfo(provider.createCommit("commit9"), project)
        branchInfo.setCommits(listOf(commit9, commit8, commit7, commit6, commit5, commit3, commit2, commit1))

        listener.keyPressed(upEvent)
        listener.keyPressed(shiftUpEvent)
        listener.keyPressed(shiftUpEvent)
        listener.keyPressed(shiftUpEvent)
        listener.keyPressed(shiftUpEvent)
        listener.keyPressed(shiftUpEvent)

        assertThat(branchInfo.selectedCommits.size).isEqualTo(6)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit1)
        assertThat(branchInfo.selectedCommits[1]).isEqualTo(commit5)
        assertThat(branchInfo.selectedCommits[2]).isEqualTo(commit6)
        assertThat(branchInfo.selectedCommits[3]).isEqualTo(commit7)
        assertThat(branchInfo.selectedCommits[4]).isEqualTo(commit8)
    }

    fun testShiftUpRemoveFromSelectedCommits() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))

        listener.keyPressed(upEvent)
        commit2.isSelected = true
        listener.keyPressed(shiftUpEvent)
        assertThat(branchInfo.selectedCommits.size).isEqualTo(0)
    }

    fun testShiftDown() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))

        listener.keyPressed(shiftDownEvent)
        listener.keyPressed(shiftDownEvent)

        assertThat(branchInfo.selectedCommits.size).isEqualTo(2)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit4)
        assertThat(branchInfo.selectedCommits[1]).isEqualTo(commit3)
    }

    fun testShiftDownCaps() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))

        listener.keyPressed(downEvent)
        listener.keyPressed(shiftDownEvent)
        listener.keyPressed(shiftDownEvent)
        listener.keyPressed(shiftDownEvent)
        listener.keyPressed(shiftDownEvent)

        assertThat(branchInfo.selectedCommits.size).isEqualTo(4)
        assertThat(branchInfo.selectedCommits[0]).isEqualTo(commit4)
        assertThat(branchInfo.selectedCommits[1]).isEqualTo(commit3)
        assertThat(branchInfo.selectedCommits[2]).isEqualTo(commit2)
        assertThat(branchInfo.selectedCommits[3]).isEqualTo(commit1)
    }

    fun testShiftDownCollapsed() {
        branchInfo.clearSelectedCommits()
        var provider = TestGitCommitProvider(project)
        var commit5 = CommitInfo(provider.createCommit("commit5"), project)
        var commit6 = CommitInfo(provider.createCommit("commit6"), project)
        var commit7 = CommitInfo(provider.createCommit("commit7"), project)
        var commit8 = CommitInfo(provider.createCommit("commit5"), project)
        var commit9 = CommitInfo(provider.createCommit("commit9"), project)
        branchInfo.setCommits(listOf(commit9, commit8, commit7, commit6, commit5, commit3, commit2, commit1))

        listener.keyPressed(downEvent)
        listener.keyPressed(downEvent)
        listener.keyPressed(downEvent)
        listener.keyPressed(downEvent)
        listener.keyPressed(downEvent)
        listener.keyPressed(shiftDownEvent)
        listener.keyPressed(shiftDownEvent)

        assertThat(branchInfo.selectedCommits.size).isEqualTo(2)
    }

    fun testShiftDownIsSelected() {
        branchInfo.clearSelectedCommits()
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))

        listener.keyPressed(downEvent)
        commit3.isSelected = true
        listener.keyPressed(shiftDownEvent)
        assertThat(branchInfo.selectedCommits.size).isEqualTo(0)
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

    fun testRightAddedBranchNull() {
        modelService.graphInfo.addedBranch = null
        branchInfo.setCommits(listOf(commit4, commit3, commit2, commit1))
        listener.keyPressed(downEvent)
        listener.keyPressed(rightEvent)
        assertThat(modelService.graphInfo.addedBranch).isNull()
        assertThat(branchInfo.selectedCommits.size).isEqualTo(1)
    }

    fun testRightAddedBranchNotNull() {
        modelService.graphInfo.addedBranch = BranchInfo("added branch", listOf(commit3, commit4))
        branchInfo.setCommits(listOf(commit2, commit1))
        listener.keyPressed(downEvent)
        listener.keyPressed(rightEvent)
        assertThat(modelService.graphInfo.addedBranch).isNotNull
        assertThat(branchInfo.selectedCommits.size).isEqualTo(0)
        assertThat(modelService.graphInfo.addedBranch?.selectedCommits?.get(0)).isEqualTo(commit4)

        listener.keyPressed(rightEvent)
        assertThat(modelService.graphInfo.addedBranch?.selectedCommits?.get(0)).isEqualTo(commit4)
    }

    fun testRightAddedBranchNotNullNoSelectedCommits() {
        modelService.graphInfo.addedBranch = BranchInfo("added branch", listOf(commit3, commit4))
        branchInfo.setCommits(listOf(commit2, commit1))
        listener.keyPressed(rightEvent)
        assertThat(modelService.graphInfo.addedBranch).isNotNull
        assertThat(branchInfo.selectedCommits.size).isEqualTo(0)
        assertThat(modelService.graphInfo.addedBranch?.selectedCommits?.size).isEqualTo(0)
    }

    fun testCurrentlyOnAddedBranch() {
        modelService.graphInfo.addedBranch = BranchInfo("added branch", listOf(commit3, commit4))
        branchInfo.setCommits(listOf(commit2, commit1))
        listener.keyPressed(downEvent)
        listener.keyPressed(rightEvent)
        assertThat(modelService.graphInfo.addedBranch).isNotNull
        assertThat(branchInfo.selectedCommits.size).isEqualTo(0)
        assertThat(modelService.graphInfo.addedBranch?.selectedCommits?.get(0)).isEqualTo(commit4)

        listener.keyPressed(leftEvent)
        assertThat(modelService.graphInfo.addedBranch?.selectedCommits?.size).isEqualTo(0)
        assertThat(branchInfo.selectedCommits.size).isEqualTo(1)
    }

    fun testLeftAlreadyOnMainBranch() {
        branchInfo.setCommits(listOf(commit2, commit1))
        val actionService = project.service<ActionService>()
        val sidePanel = actionService.mainPanel.sidePanel
        actionService.mainPanel.sidePanel.sideBranchPanels.clear()
        sidePanel.sideBranchPanels.add(0, SideBranchPanel("haha", project))
        listener.keyPressed(downEvent)
        listener.keyPressed(leftEvent)
        assertThat(sidePanel.listener.selected?.branchName).isEqualTo(sidePanel.sideBranchPanels[0].branchName)
    }

    fun testLeftSelectedBranchIsNoneOfTheTwo() {
        branchInfo.setCommits(listOf(commit2, commit1))
        val actionService = project.service<ActionService>()
        val sidePanel = actionService.mainPanel.sidePanel
        sidePanel.sideBranchPanels.add(0, SideBranchPanel("haha", project))
        listener.keyPressed(downEvent)
        modelService.graphInfo.mainBranch = BranchInfo("testing")
        listener.keyPressed(leftEvent)
        assertThat(modelService.graphInfo.mainBranch.selectedCommits.size).isEqualTo(0)
    }

    fun testEscape() {
        branchInfo.setCommits(listOf(commit2, commit1))
        listener.keyPressed(downEvent)
        val esc =
            KeyEvent(
                mainPanel,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                KeyEvent.SHIFT_DOWN_MASK,
                KeyEvent.VK_ESCAPE,
                KeyEvent.CHAR_UNDEFINED,
            )
        listener.keyPressed(esc)
        assertThat(branchInfo.selectedCommits.size).isEqualTo(0)
    }

    fun testEventIsNull() {
        branchInfo.setCommits(listOf(commit2, commit1))
        listener.keyPressed(null)
        assertThat(branchInfo.selectedCommits.size).isEqualTo(0)
    }

    fun testLeftWithAlt() {
        leftEvent =
            KeyEvent(
                mainPanel,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                KeyEvent.ALT_DOWN_MASK,
                KeyEvent.VK_LEFT,
                KeyEvent.CHAR_UNDEFINED,
            )
        branchInfo.setCommits(listOf(commit2, commit1))
        val actionService = project.service<ActionService>()
        val sidePanel = actionService.mainPanel.sidePanel
        actionService.mainPanel.sidePanel.sideBranchPanels.clear()
        sidePanel.sideBranchPanels.add(0, SideBranchPanel("haha", project))
        listener.keyPressed(downEvent)
        listener.keyPressed(leftEvent)
        assertThat(sidePanel.listener.selected?.branchName).isEqualTo(sidePanel.sideBranchPanels[0].branchName)
    }

    fun testRightWithAlt() {
        rightEvent =
            KeyEvent(
                mainPanel,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                KeyEvent.ALT_DOWN_MASK,
                KeyEvent.VK_RIGHT,
                KeyEvent.CHAR_UNDEFINED,
            )

        modelService.graphInfo.addedBranch = BranchInfo("added branch", listOf(commit3, commit4))
        branchInfo.setCommits(listOf(commit2, commit1))
        listener.keyPressed(rightEvent)
        assertThat(modelService.graphInfo.addedBranch).isNotNull
        assertThat(branchInfo.selectedCommits.size).isEqualTo(0)
        assertThat(modelService.graphInfo.addedBranch?.selectedCommits?.size).isEqualTo(0)
    }

    fun testLeftWithAnyOtherKey() {
        leftEvent =
            KeyEvent(
                mainPanel,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                0,
                KeyEvent.VK_LEFT,
                KeyEvent.CHAR_UNDEFINED,
            )
        branchInfo.setCommits(listOf(commit2, commit1))
        val actionService = project.service<ActionService>()
        val sidePanel = actionService.mainPanel.sidePanel
        actionService.mainPanel.sidePanel.sideBranchPanels.clear()
        sidePanel.sideBranchPanels.add(0, SideBranchPanel("haha", project))
        listener.keyPressed(downEvent)
        listener.keyPressed(leftEvent)
        assertThat(sidePanel.listener.selected?.branchName).isEqualTo(sidePanel.sideBranchPanels[0].branchName)
    }

    fun testRightWithAnyOtherKey() {
        rightEvent =
            KeyEvent(
                mainPanel,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                0,
                KeyEvent.VK_RIGHT,
                KeyEvent.CHAR_UNDEFINED,
            )

        modelService.graphInfo.addedBranch = BranchInfo("added branch", listOf(commit3, commit4))
        branchInfo.setCommits(listOf(commit2, commit1))
        listener.keyPressed(rightEvent)
        assertThat(modelService.graphInfo.addedBranch).isNotNull
        assertThat(branchInfo.selectedCommits.size).isEqualTo(0)
        assertThat(modelService.graphInfo.addedBranch?.selectedCommits?.size).isEqualTo(0)
    }

    fun testAnyOtherKey() {
        rightEvent =
            KeyEvent(
                mainPanel,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                0,
                1,
                KeyEvent.CHAR_UNDEFINED,
            )

        modelService.graphInfo.addedBranch = BranchInfo("added branch", listOf(commit3, commit4))
        branchInfo.setCommits(listOf(commit2, commit1))
        listener.keyPressed(rightEvent)
        assertThat(branchInfo.selectedCommits.size).isEqualTo(0)
    }

    fun testAnyOtherKeyWithAltDown() {
        rightEvent =
            KeyEvent(
                mainPanel,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                KeyEvent.ALT_DOWN_MASK,
                1,
                KeyEvent.CHAR_UNDEFINED,
            )

        modelService.graphInfo.addedBranch = BranchInfo("added branch", listOf(commit3, commit4))
        branchInfo.setCommits(listOf(commit2, commit1))
        listener.keyPressed(rightEvent)
        assertThat(branchInfo.selectedCommits.size).isEqualTo(0)
    }

    fun testNoBodyMethods() {
        val listener = BranchNavigationListener(project, modelService)
        listener.keyReleased(upEvent)
        listener.keyTyped(downEvent)
        listener.dispose()

        assertThat(branchInfo.selectedCommits.size).isEqualTo(0)
    }

    fun testAltUpWithSecondBranchSelected() {
        modelService.graphInfo.addedBranch = BranchInfo("added branch", listOf(commit3, commit4))
        branchInfo.setCommits(listOf(commit2, commit1))
        modelService.graphInfo.addedBranch?.selectedCommits?.add(0, commit3)
        listener.altUp()
        assertThat(modelService.graphInfo.addedBranch?.selectedCommits?.size).isEqualTo(1)
    }

    fun testAltUpWithSquashed() {
        branchInfo.setCommits(listOf(commit3, commit2, commit1))
        commit3.isSquashed = true
        branchInfo.selectedCommits = mutableListOf(commit3)
        listener.altUp()
        assertThat(branchInfo.currentCommits).isEqualTo(branchInfo.initialCommits)
    }

    fun testAltDownWithSecondBranchSelected() {
        modelService.graphInfo.addedBranch = BranchInfo("added branch", listOf(commit3, commit4))
        branchInfo.setCommits(listOf(commit2, commit1))
        modelService.graphInfo.addedBranch?.selectedCommits?.add(0, commit3)
        listener.altDown()
        assertThat(modelService.graphInfo.addedBranch?.selectedCommits?.size).isEqualTo(1)
    }

    fun testAltDownWithSquashed() {
        branchInfo.setCommits(listOf(commit3, commit2, commit1))
        commit3.isSquashed = true
        branchInfo.selectedCommits = mutableListOf(commit3)
        listener.altDown()
        assertThat(branchInfo.currentCommits).isEqualTo(branchInfo.initialCommits)
    }
}
