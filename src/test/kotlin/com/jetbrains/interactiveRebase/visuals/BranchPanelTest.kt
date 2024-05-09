package com.jetbrains.interactiveRebase.visuals

import com.intellij.ui.JBColor
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.plaf.ComponentUI

class BranchPanelTest {
    private lateinit var graph: Graphics2D
    private lateinit var graph2: Graphics
    private lateinit var ui: ComponentUI
    private lateinit var branchPanel: BranchPanel

    @Before
    fun setUp() {
        graph = mock(Graphics2D::class.java)
        graph2 = mock(Graphics::class.java)
        ui = mock(ComponentUI::class.java)
        branchPanel = BranchPanel(Branch(true, "branch", listOf("a", "b", "c")), JBColor.BLUE)
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

    @Test
    fun testColor() {
        assertEquals(branchPanel.color, JBColor.BLUE)
    }

    @Test
    fun testBranchSize() {
        assertEquals(branchPanel.borderSize, 1f)
    }
}
