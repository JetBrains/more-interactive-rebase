package com.jetbrains.interactiveRebase.visuals

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import junit.framework.TestCase
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point

class DragPanelTest : BasePlatformTestCase() {
    private lateinit var dragPanel: DragPanel

    override fun setUp() {
        super.setUp()
        dragPanel = spy(DragPanel())
    }

    fun testCorrectLayout() {
        TestCase.assertNull(dragPanel.layout)
    }

    fun testPaintComponentTrueNull() {
        dragPanel.labelIsDragged = true

        val g: Graphics = mock(Graphics::class.java)
        val g2d = mock(Graphics2D::class.java)
        `when`(g2d.create()).thenReturn(g)

        org.junit.jupiter.api.assertThrows<NullPointerException> {
            dragPanel.paintComponent(g2d)
        }
    }

    fun testPaintComponentFalse() {
        val g: Graphics = mock(Graphics::class.java)
        val g2d = mock(Graphics2D::class.java)
        `when`(g2d.create()).thenReturn(g)

        dragPanel.labelIsDragged = false
        dragPanel.startDragPoint = Point(5, 5)
        dragPanel.endDragPoint = Point(10, 10)

        dragPanel.paintComponent(g2d)

        verify(dragPanel, never()).drawCurvedArrow(
            g2d,
            Point(5, 5),
            Point(10, 10),
        )
    }

    fun testPaintComponentActuallyDraws() {
        dragPanel.labelIsDragged = true
        dragPanel.startDragPoint = Point(5, 5)
        dragPanel.endDragPoint = Point(10, 10)

        val g: Graphics = mock(Graphics::class.java)
        val g2d = mock(Graphics2D::class.java)
        `when`(g2d.create()).thenReturn(g)

        dragPanel.paintComponent(g2d)

        verify(dragPanel).drawCurvedArrow(
            g2d,
            Point(5, 5),
            Point(10, 10),
        )
    }
}
