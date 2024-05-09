package com.jetbrains.interactiveRebase.visuals

import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import git4idea.GitCommit
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.plaf.ComponentUI

class BranchPanelTest {
    private lateinit var graph: Graphics2D
    private lateinit var graph2: Graphics
    private lateinit var commit1: CommitInfo
    private lateinit var commit2: CommitInfo
    private lateinit var commit3: CommitInfo
    private lateinit var ui: ComponentUI
    private lateinit var branchPanel: BranchPanel

    @Before
    fun setUp() {
        graph = mock(Graphics2D::class.java)
        graph2 = mock(Graphics::class.java)
        ui = mock(ComponentUI::class.java)
        commit1 = CommitInfo(mock(GitCommit::class.java), null)
        commit2 = CommitInfo(mock(GitCommit::class.java), null)
        commit3 =  CommitInfo(mock(GitCommit::class.java), null)
        branchPanel = BranchPanel(BranchInfo("branch", mutableListOf(commit1, commit2, commit3)), JBColor.BLUE)
    }

    @Test
    fun testPaintComponent() {
        `when`(graph.create()).thenReturn(graph2)

        branchPanel.paintComponent(graph)

        verify(graph2).dispose()
        verify(graph, times(1)).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(graph, times(2)).drawLine(anyInt(), anyInt(), anyInt(), anyInt())
    }

    @Test
    fun testGetCirclePanels() {
        assertEquals(branchPanel.getCirclePanels().size, 3)
    }
}
