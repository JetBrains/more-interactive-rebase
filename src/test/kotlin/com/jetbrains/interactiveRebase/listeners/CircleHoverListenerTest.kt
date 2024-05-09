package com.jetbrains.interactiveRebase.listeners

import CirclePanel
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D

class CircleHoverListenerTest : BasePlatformTestCase() {
    private lateinit var circlePanel: CirclePanel
    private lateinit var listener: CircleHoverListener

    override fun setUp() {
        super.setUp()
        circlePanel = mock(CirclePanel::class.java)
        listener = CircleHoverListener(circlePanel)
    }

    fun testMouseEnteredInsideCircle() {
        val event = mock(MouseEvent::class.java)
        `when`(event.x).thenReturn(10)
        `when`(event.y).thenReturn(10)
        `when`(circlePanel.circle).thenReturn(Ellipse2D.Double(0.0, 0.0, 20.0, 20.0))

        listener.mouseEntered(event)

        verify(circlePanel).isHovering = true
        verify(circlePanel).repaint()
    }

    fun testMouseEnteredOutsideCircle() {
        val event = mock(MouseEvent::class.java)
        `when`(event.x).thenReturn(10)
        `when`(event.y).thenReturn(10)
        `when`(circlePanel.circle).thenReturn(Ellipse2D.Double(10.0, 10.0, 20.0, 20.0))

        listener.mouseEntered(event)
        verify(circlePanel, never()).repaint()
    }

    fun testMouseEnteredNullEvent() {
        val event = mock(MouseEvent::class.java)
        `when`(event.x).thenReturn(10)
        `when`(event.y).thenReturn(10)
        `when`(circlePanel.circle).thenReturn(Ellipse2D.Double(10.0, 10.0, 20.0, 20.0))

        listener.mouseEntered(null)
        verify(circlePanel, never()).repaint()
    }

    fun testMouseExitedOutsideCircle() {
        val event = mock(MouseEvent::class.java)
        `when`(event.x).thenReturn(10)
        `when`(event.y).thenReturn(10)
        `when`(circlePanel.circle).thenReturn(Ellipse2D.Double(10.0, 10.0, 20.0, 20.0))

        listener.mouseExited(event)

        verify(circlePanel).isHovering = false
        verify(circlePanel).repaint()
    }

    fun testMouseExitedInsideCircle() {
        val event = mock(MouseEvent::class.java)
        `when`(event.x).thenReturn(10)
        `when`(event.y).thenReturn(10)
        `when`(circlePanel.circle).thenReturn(Ellipse2D.Double(0.0, 0.0, 20.0, 20.0))
        listener.mouseExited(event)
        verify(circlePanel, never()).repaint()
    }

    fun testMouseExitedNullEvent() {
        val event = mock(MouseEvent::class.java)
        `when`(event.x).thenReturn(10)
        `when`(event.y).thenReturn(10)
        `when`(circlePanel.circle).thenReturn(Ellipse2D.Double(0.0, 0.0, 20.0, 20.0))
        listener.mouseExited(null)
        verify(circlePanel, never()).repaint()
    }

    fun testMouseClicked() {
        `when`(circlePanel.isSelected).thenReturn(false)
        listener.mouseClicked(null)
        verify(circlePanel).isSelected = true
        verify(circlePanel).repaint()
    }

    fun testMouseMovedInsideCircle() {
        val event = mock(MouseEvent::class.java)
        `when`(event.x).thenReturn(10)
        `when`(event.y).thenReturn(10)
        `when`(circlePanel.circle).thenReturn(Ellipse2D.Double(0.0, 0.0, 20.0, 20.0))
        listener.mouseMoved(event)

        verify(circlePanel).isHovering = true
        verify(circlePanel).repaint()
    }

    fun testMouseMovedOutsideCircle() {
        val event = mock(MouseEvent::class.java)
        `when`(event.x).thenReturn(100)
        `when`(event.y).thenReturn(100)
        `when`(circlePanel.circle).thenReturn(Ellipse2D.Double(10.0, 10.0, 20.0, 20.0))
        listener.mouseMoved(event)

        verify(circlePanel).isHovering = false
        verify(circlePanel).repaint()
    }

    fun testUnsupportedOperations() {
        val event = mock(MouseEvent::class.java)

        listOf(
            { listener.mousePressed(event) },
            { listener.mouseReleased(event) },
            { listener.mouseDragged(event) },
        ).forEach { testOperation ->
            try {
                testOperation.invoke()
                Assert.fail("Expected UnsupportedOperationException was not thrown")
            } catch (e: UnsupportedOperationException) {
                // The expected behavior of these dummy methods is to do nothing other than throw an exception.
            }
        }
    }
}
