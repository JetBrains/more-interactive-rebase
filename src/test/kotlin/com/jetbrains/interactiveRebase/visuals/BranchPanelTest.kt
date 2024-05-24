package com.jetbrains.interactiveRebase.visuals

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.StopToEditCommand
import git4idea.GitCommit
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.plaf.ComponentUI

class BranchPanelTest : BasePlatformTestCase() {
    private lateinit var graph: Graphics2D
    private lateinit var graph2: Graphics
    private lateinit var commit1: CommitInfo
    private lateinit var commit2: CommitInfo
    private lateinit var commit3: CommitInfo
    private lateinit var ui: ComponentUI
    private lateinit var branchPanel: BranchPanel

    override fun setUp() {
        super.setUp()
        graph = mock(Graphics2D::class.java)
        graph2 = mock(Graphics::class.java)
        ui = mock(ComponentUI::class.java)
        commit1 = CommitInfo(mock(GitCommit::class.java), project, mutableListOf())
        commit2 = CommitInfo(mock(GitCommit::class.java), project, mutableListOf())
        commit3 = CommitInfo(mock(GitCommit::class.java), project, mutableListOf())
        branchPanel = BranchPanel(BranchInfo("branch", mutableListOf(commit1, commit2, commit3)), JBColor.BLUE)

        commit2.changes.add(StopToEditCommand(commit2))
        commit3.changes.add(DropCommand(commit3))
    }

    fun testPaintComponent() {
        `when`(graph.create()).thenReturn(graph2)

        branchPanel.paintComponent(graph)

        verify(graph2).dispose()
        verify(graph, times(1)).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(graph, times(4)).drawLine(anyInt(), anyInt(), anyInt(), anyInt())
    }

    fun testGetCirclePanels() {
        assertEquals(branchPanel.circles.size, 3)
    }

    fun testColor() {
        assertEquals(branchPanel.color, JBColor.BLUE)
    }

    fun testBranchSize() {
        assertEquals(branchPanel.borderSize, 1f)
    }

    fun testInitializeCirclePanelDrop() {
        val circle = branchPanel.initializeCirclePanel(2)
        assertTrue(circle is DropCirclePanel)
        assertEquals(circle.commit, commit3)
    }

    fun testInitializeCirclePanelStopToEdit() {
        val circle = branchPanel.initializeCirclePanel(1)
        assertTrue(circle is StopToEditCirclePanel)
        assertEquals(circle.commit, commit2)
    }
}
