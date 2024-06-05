package com.jetbrains.interactiveRebase.listeners

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.visuals.Palette
import com.jetbrains.interactiveRebase.visuals.multipleBranches.SideBranchPanel
import com.jetbrains.interactiveRebase.visuals.multipleBranches.SidePanel
import org.assertj.core.api.Assertions.assertThat
import java.awt.event.MouseEvent

class SideBranchPanelListenerTest : BasePlatformTestCase() {
    lateinit var sideBranchPanel: SideBranchPanel
    lateinit var parent: SidePanel
    lateinit var sideBranchPanelListener: SideBranchPanelListener
    lateinit var mouseEvent: MouseEvent

    override fun setUp() {
        super.setUp()
        sideBranchPanel = SideBranchPanel("main")
        parent = SidePanel(mutableListOf("feature", "bugfix"))
        parent.sideBranchPanels.add(sideBranchPanel)
        parent.sideBranchPanels.add(SideBranchPanel("feature"))
        parent.sideBranchPanels.add(SideBranchPanel("bugfix"))
        sideBranchPanelListener = SideBranchPanelListener(sideBranchPanel, parent)
        mouseEvent = MouseEvent(sideBranchPanel, 0, 0, 0, 0, 0, 0, false)
    }

    fun testMouseEnteredCanSelect() {
        sideBranchPanelListener.mouseEntered(mouseEvent)

        assertThat(sideBranchPanel.backgroundColor).isEqualTo(Palette.JETBRAINSHOVER)
    }

    fun testMouseEnteredCanNotSelect() {
        sideBranchPanel.isSelected = true
        sideBranchPanelListener.mouseEntered(mouseEvent)

        assertThat(sideBranchPanel.backgroundColor).isNotEqualTo(Palette.JETBRAINSHOVER)
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
        sideBranchPanelListener.mouseClicked(mouseEvent)
        assertThat(sideBranchPanel.isSelected).isTrue()
        assertThat(sideBranchPanel.backgroundColor).isEqualTo(Palette.JETBRAINSSELCTED)
        assertThat(parent.sideBranchPanels[1].label.foreground).isEqualTo(Palette.GRAY)
    }

    fun testMouseClickedCanNotSelectBranch() {
        parent.sideBranchPanels[1].isSelected = true
        sideBranchPanelListener.mouseClicked(mouseEvent)
        assertThat(sideBranchPanel.isSelected).isFalse()
        assertThat(sideBranchPanel.backgroundColor).isNotEqualTo(Palette.JETBRAINSSELCTED)
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
