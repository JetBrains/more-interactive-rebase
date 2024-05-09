package com.jetbrains.interactiveRebase.visuals

import CirclePanel
import com.intellij.ui.JBColor
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.plaf.ComponentUI

class CirclePanelTest {
    private lateinit var graph: Graphics2D
    private lateinit var graph2: Graphics
    private lateinit var ui: ComponentUI

    @Before
    fun setUp() {
        graph = mock(Graphics2D::class.java)
        graph2 = mock(Graphics::class.java)
        ui = mock(ComponentUI::class.java)
    }

    @Test
    fun testPaintComponent() {
        val circlePanel = CirclePanel(10.0, 2f, JBColor.BLUE)
        `when`(graph.create()).thenReturn(graph2)

        circlePanel.paintComponent(graph)

        verify(graph2).dispose()
        verify(graph, times(1)).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(graph).fill(circlePanel.circle)
    }

    @Test
    fun testPaintComponentIsSelected() {
        val circlePanel = CirclePanel(10.0, 2f, JBColor.BLUE)
        `when`(graph.create()).thenReturn(graph2)
        circlePanel.isSelected = true
        circlePanel.isHovering = true
        circlePanel.paintComponent(graph)

        verify(graph2).dispose()
        verify(graph, times(1)).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(graph).draw(circlePanel.circle)
        verify(graph).fill(circlePanel.circle)
    }
}
