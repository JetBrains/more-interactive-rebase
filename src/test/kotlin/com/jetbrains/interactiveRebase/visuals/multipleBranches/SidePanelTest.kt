package com.jetbrains.interactiveRebase.visuals.multipleBranches

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.jetbrains.interactiveRebase.visuals.Palette
import org.assertj.core.api.Assertions.assertThat
import java.awt.GridBagConstraints

class SidePanelTest : BasePlatformTestCase() {
    val branches = mutableListOf("Branch 1", "Branch 2", "Branch 3")

    fun testSetVisible() {
        val sidePanel = SidePanel()
        sidePanel.isVisible = true
        assertTrue(sidePanel.isVisible)
    }

    fun testUpdateBranchNames() {
        val sidePanel = SidePanel()
        assertThat(sidePanel.sideBranchPanels.size).isEqualTo(3)
        assertThat(sidePanel.sideBranchPanels[0].branchName).isEqualTo("Branch 1")
        assertThat(sidePanel.sideBranchPanels[1].branchName).isEqualTo("Branch 2")
        assertThat(sidePanel.sideBranchPanels[2].branchName).isEqualTo("Branch 3")

        assertThat(sidePanel.sideBranchPanels[0].mouseListeners.size).isEqualTo(1)
        assertThat(sidePanel.sideBranchPanels[1].mouseListeners.size).isEqualTo(1)
        assertThat(sidePanel.sideBranchPanels[2].mouseListeners.size).isEqualTo(1)

        assertThat(sidePanel.sideBranchPanels[0].mouseMotionListeners.size).isEqualTo(1)
        assertThat(sidePanel.sideBranchPanels[1].mouseMotionListeners.size).isEqualTo(1)
        assertThat(sidePanel.sideBranchPanels[2].mouseMotionListeners.size).isEqualTo(1)
    }

    fun testGetAlignmentForBranch() {
        val sidePanel = SidePanel()
        val gbc = sidePanel.getAlignmentForBranch(0)
        assertThat(gbc.gridx).isEqualTo(0)
        assertThat(gbc.gridy).isEqualTo(0)
        assertThat(gbc.weighty).isEqualTo(0.0)
        assertThat(gbc.weightx).isEqualTo(1.0)
        assertThat(gbc.fill).isEqualTo(GridBagConstraints.HORIZONTAL)
        assertThat(gbc.anchor).isEqualTo(GridBagConstraints.NORTH)
        assertThat(gbc.insets).isEqualTo(JBUI.insets(2, 4, 1, 4))
    }

    fun testGetAlignmentForBranchLastBRanchName() {
        val sidePanel = SidePanel()
        val gbc = sidePanel.getAlignmentForBranch(branches.size - 1)
        assertThat(gbc.gridx).isEqualTo(0)
        assertThat(gbc.gridy).isEqualTo(2)
        assertThat(gbc.weighty).isEqualTo(1.0)
        assertThat(gbc.weightx).isEqualTo(1.0)
        assertThat(gbc.fill).isEqualTo(GridBagConstraints.HORIZONTAL)
        assertThat(gbc.anchor).isEqualTo(GridBagConstraints.NORTH)
        assertThat(gbc.insets).isEqualTo(JBUI.insets(2, 4, 1, 4))
    }

    fun testCanSelectBranchIsAlreadySelected() {
        val sidePanel = SidePanel()
        sidePanel.sideBranchPanels[0].isSelected = true
        assertThat(sidePanel.canSelectBranch(sidePanel.sideBranchPanels[0])).isFalse()
    }

    fun testCanSelectBranchIsAnotherSelected() {
        val sidePanel = SidePanel()
        sidePanel.sideBranchPanels[0].isSelected = true

        assertThat(sidePanel.canSelectBranch(sidePanel.sideBranchPanels[1])).isFalse()
    }

    fun testCanSelectBranchIsNotSelected() {
        val sidePanel = SidePanel()

        assertThat(sidePanel.canSelectBranch(sidePanel.sideBranchPanels[0])).isTrue()
    }

    fun testCanSelectBranchEmptyList() {
        val sidePanel = SidePanel()

        sidePanel.branches.clear()

        assertThat(sidePanel.canSelectBranch(SideBranchPanel("Branch 1"))).isTrue()
    }

    fun testMakeBranchesUnavailable() {
        val sidePanel = SidePanel()

        sidePanel.sideBranchPanels[0].isSelected = true
        sidePanel.makeBranchesUnavailableExceptCurrent(sidePanel.sideBranchPanels[0])

        assertThat(sidePanel.sideBranchPanels[0].label.foreground).isEqualTo(JBColor.BLACK)
        assertThat(sidePanel.sideBranchPanels[1].label.foreground).isEqualTo(Palette.GRAYBUTTON)
        assertThat(sidePanel.sideBranchPanels[2].label.foreground).isEqualTo(Palette.GRAYBUTTON)
    }

    fun testResetAllBranchesVisually() {
        val sidePanel = SidePanel()
        sidePanel.resetAllBranchesVisually()
        assertThat(sidePanel.sideBranchPanels[0].backgroundColor).isEqualTo(Palette.TRANSPARENT)
        assertThat(sidePanel.sideBranchPanels[1].backgroundColor).isEqualTo(Palette.TRANSPARENT)
        assertThat(sidePanel.sideBranchPanels[2].backgroundColor).isEqualTo(Palette.TRANSPARENT)
    }
}
