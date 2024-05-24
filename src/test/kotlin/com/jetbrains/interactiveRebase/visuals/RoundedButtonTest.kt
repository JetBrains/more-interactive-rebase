package com.jetbrains.interactiveRebase.visuals

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.plaf.ComponentUI

class RoundedButtonTest : BasePlatformTestCase() {
    lateinit var roundedButton: RoundedButton
    private lateinit var graph: Graphics2D
    private lateinit var graph2: Graphics
    private lateinit var ui: ComponentUI

    override fun setUp() {
        super.setUp()
        roundedButton = RoundedButton("Rebase", Palette.DARKBLUE, Palette.WHITETEXT)
        graph = mock(Graphics2D::class.java)
        graph2 = mock(Graphics::class.java)
        `when`(graph.create()).thenReturn(graph2)
        ui = mock(ComponentUI::class.java)
    }

    fun testPaintComponent() {
        roundedButton.paintComponent(graph)
        verify(graph2).dispose()
        verify(graph, times(1)).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        assertEquals(roundedButton.isContentAreaFilled, false)
        assertEquals(roundedButton.text, "Rebase")
        assertEquals(roundedButton.background, Palette.DARKBLUE)
        assertEquals(roundedButton.foreground, Palette.WHITETEXT)
    }
}
