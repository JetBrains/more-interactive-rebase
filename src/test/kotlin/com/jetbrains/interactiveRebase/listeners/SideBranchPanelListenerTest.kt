package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.DialogService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import com.jetbrains.interactiveRebase.visuals.Palette
import com.jetbrains.interactiveRebase.visuals.multipleBranches.SideBranchPanel
import com.jetbrains.interactiveRebase.visuals.multipleBranches.SidePanel
import org.assertj.core.api.Assertions.assertThat
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.awt.event.MouseEvent

class SideBranchPanelListenerTest : BasePlatformTestCase() {
    lateinit var sideBranchPanel: SideBranchPanel
    lateinit var parent: SidePanel
    lateinit var sideBranchPanelListener: SideBranchPanelListener
    lateinit var mouseEvent: MouseEvent

    override fun setUp() {
        super.setUp()
        sideBranchPanel = SideBranchPanel("main", project)
        parent = SidePanel(mutableListOf("feature", "bugfix"), project)
        parent.sideBranchPanels.add(sideBranchPanel)
        parent.sideBranchPanels.add(SideBranchPanel("feature", project))
        parent.sideBranchPanels.add(SideBranchPanel("bugfix", project))
        sideBranchPanelListener = SideBranchPanelListener(sideBranchPanel, parent)
        mouseEvent = MouseEvent(sideBranchPanel, 0, 0, 0, 0, 0, 0, false)
    }

    fun testMouseEnteredCanSelect() {
        sideBranchPanelListener.mouseEntered(mouseEvent)

        assertThat(sideBranchPanel.backgroundColor).isEqualTo(Palette.JETBRAINS_HOVER)
    }

    fun testMouseEnteredCanNotSelect() {
        sideBranchPanel.isSelected = true
        sideBranchPanelListener.mouseEntered(mouseEvent)

        assertThat(sideBranchPanel.backgroundColor).isNotEqualTo(Palette.JETBRAINS_HOVER)
    }

    fun testMouseExitedNoSelectedBranch() {
        sideBranchPanelListener.mouseExited(mouseEvent)

        assertThat(parent.sideBranchPanels[0].backgroundColor).isEqualTo(Palette.TRANSPARENT)
        assertThat(parent.sideBranchPanels[1].backgroundColor).isEqualTo(Palette.TRANSPARENT)
        assertThat(parent.sideBranchPanels[2].backgroundColor).isEqualTo(Palette.TRANSPARENT)
    }

    fun testMouseExitedSelectedBranch() {
        sideBranchPanel.isSelected = true
        sideBranchPanelListener.mouseExited(mouseEvent)

        assertThat(parent.sideBranchPanels[0].backgroundColor).isNotEqualTo(Palette.TRANSPARENT)
    }

    fun testMouseClickedCanSelectBranch() {
        project.service<RebaseInvoker>().addCommand(
            DropCommand(CommitInfo(TestGitCommitProvider(project).createCommit("add test"), project)),
        )
        val dialog: DialogService = Mockito.mock(DialogService::class.java)
        Mockito.`when`(dialog.warningYesNoDialog(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(true)
        val modelService = Mockito.mock(ModelService::class.java)
        val controlledSideBranch = SideBranchPanel("main", project, dialog, modelService)
        val controlledSideListener = SideBranchPanelListener(controlledSideBranch, parent)
        controlledSideListener.mouseClicked(mouseEvent)
        assertThat(controlledSideBranch.isSelected).isTrue()
        assertThat(controlledSideBranch.backgroundColor).isEqualTo(Palette.JETBRAINS_SELECTED)
        assertThat(parent.sideBranchPanels[1].label.foreground).isEqualTo(Palette.GRAY)
    }

    fun testMouseClickedCanSelectBranchDeclinedWarning() {
        project.service<RebaseInvoker>().addCommand(
            DropCommand(CommitInfo(TestGitCommitProvider(project).createCommit("add test"), project)),
        )
        val dialog: DialogService = Mockito.mock(DialogService::class.java)
        Mockito.`when`(dialog.warningYesNoDialog(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(false)
        val modelService = Mockito.mock(ModelService::class.java)
        val controlledSideBranch = SideBranchPanel("main", project, dialog, modelService)
        val controlledSideListener = SideBranchPanelListener(controlledSideBranch, parent)
        controlledSideListener.mouseClicked(mouseEvent)
        assertThat(sideBranchPanel.isSelected).isFalse()
        assertThat(sideBranchPanel.backgroundColor).isNotEqualTo(Palette.JETBRAINS_SELECTED)
        assertThat(parent.sideBranchPanels[1].label.foreground).isNotEqualTo(Palette.GRAY)
    }

    fun testMouseClickedCanNotSelectBranch() {
        parent.sideBranchPanels[1].isSelected = true
        sideBranchPanelListener.mouseClicked(mouseEvent)
        assertThat(sideBranchPanel.isSelected).isFalse()
        assertThat(sideBranchPanel.backgroundColor).isNotEqualTo(Palette.JETBRAINS_SELECTED)
    }

    fun testMouseClickedDeselect() {
        sideBranchPanel.isSelected = true
        sideBranchPanelListener.mouseClicked(mouseEvent)
        assertThat(sideBranchPanel.isSelected).isFalse()
        assertThat(parent.sideBranchPanels[0].backgroundColor).isEqualTo(Palette.TRANSPARENT)
        assertThat(parent.sideBranchPanels[1].backgroundColor).isEqualTo(Palette.TRANSPARENT)
        assertThat(parent.sideBranchPanels[2].backgroundColor).isEqualTo(Palette.TRANSPARENT)
    }
}
