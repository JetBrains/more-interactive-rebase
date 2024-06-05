package com.jetbrains.interactiveRebase.visuals.multipleBranches

import com.intellij.icons.AllIcons
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.visuals.Palette
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import java.awt.GridBagConstraints

class SideBranchPanelTest : BasePlatformTestCase() {
    val branchName = "main"

    fun testCreateSideBranchPanel() {
        val sideBranchPanel = SideBranchPanel(branchName)
        assertNotNull(sideBranchPanel)
        assertThat(sideBranchPanel.branchName).isEqualTo(branchName)
        assertThat(sideBranchPanel.button).isNotNull
    }

    fun testAddRemoveBranchButton() {
        val sideBranchPanel = SideBranchPanel(branchName)
        assertNotNull(sideBranchPanel)
        assertThat(sideBranchPanel.button).isNotNull
        assertThat(sideBranchPanel.button.icon).isEqualTo(AllIcons.General.Remove)
        assertThat(sideBranchPanel.button.arcHeight).isEqualTo(5)
        assertThat(sideBranchPanel.button.arcWidth).isEqualTo(5)
        assertThat(sideBranchPanel.button.isContentAreaFilled).isEqualTo(false)
        assertThat(sideBranchPanel.button.isVisible).isEqualTo(false)
        assertThat(sideBranchPanel.button.border).isNull()
    }

    fun testResetBranchPanelVisually() {
        val sideBranchPanel = SideBranchPanel(branchName)
        sideBranchPanel.resetSideBranchPanelVisually()
        assertThat(sideBranchPanel.backgroundColor).isEqualTo(Palette.TRANSPARENT)
        assertThat(sideBranchPanel.label.foreground).isEqualTo(JBColor.BLACK)
        assertThat(sideBranchPanel.button.isVisible).isEqualTo(false)
        assertThat(sideBranchPanel.isSelected).isEqualTo(false)
        assertThat(sideBranchPanel.isOpaque).isEqualTo(false)
    }

    fun testGetAlignmentForButton() {
        val sideBranchPanel = SideBranchPanel(branchName)
        val gbc = sideBranchPanel.getAlignmentForButton()
        assertThat(gbc.weightx).isEqualTo(0.1)
        assertThat(gbc.gridx).isEqualTo(1)
        assertThat(gbc.anchor).isEqualTo(GridBagConstraints.LINE_END)
        assertThat(gbc.insets.right).isEqualTo(10)
    }

    fun testOnHover() {
        val sideBranchPanel = SideBranchPanel(branchName)
        sideBranchPanel.onHover()
        assertThat(sideBranchPanel.backgroundColor).isEqualTo(Palette.JETBRAINS_HOVER)
    }

    fun testGrayOutText() {
        val sideBranchPanel = SideBranchPanel(branchName)
        sideBranchPanel.grayOutText()
        assertThat(sideBranchPanel.label.foreground).isEqualTo(Palette.GRAYBUTTON)
    }

    fun testButtonOnHover() {
        val sideBranchPanel = SideBranchPanel(branchName)
        sideBranchPanel.buttonOnHover()
        assertThat(sideBranchPanel.button.backgroundColor).isEqualTo(sideBranchPanel.background.darker())
    }

    fun testButtonOnHoverExit() {
        val sideBranchPanel = SideBranchPanel(branchName)
        sideBranchPanel.buttonOnHoverExit()
        assertThat(sideBranchPanel.button.backgroundColor).isEqualTo(Palette.TRANSPARENT)
    }
}
