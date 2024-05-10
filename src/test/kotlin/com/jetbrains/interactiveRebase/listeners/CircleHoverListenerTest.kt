package com.jetbrains.interactiveRebase.listeners

import CirclePanel
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import git4idea.GitCommit
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D

class CircleHoverListenerTest : BasePlatformTestCase() {
    private lateinit var circlePanel: CirclePanel
    private lateinit var listener: CircleHoverListener
    private lateinit var commit1: CommitInfo

    override fun setUp() {
        super.setUp()
        circlePanel = mock(CirclePanel::class.java)
        listener = CircleHoverListener(circlePanel)
        commit1 = CommitInfo(mock(GitCommit::class.java), project, null)
    }

    fun testMouseEnteredInsideCircle() {
        val event = mock(MouseEvent::class.java)
        `when`(event.x).thenReturn(10)
        `when`(event.y).thenReturn(10)
        `when`(circlePanel.circle).thenReturn(Ellipse2D.Double(0.0, 0.0, 20.0, 20.0))
        `when`(circlePanel.commit).thenReturn(commit1)

        listener.mouseEntered(event)

        verify(circlePanel).repaint()
        assertThat(circlePanel.commit.isHovered).isTrue()
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
        `when`(circlePanel.commit).thenReturn(commit1)

        listener.mouseExited(event)

        assertThat(circlePanel.commit.isHovered).isFalse()
        verify(circlePanel).repaint()
    }

    fun testMouseExitedInsideCircle() {
        val event = mock(MouseEvent::class.java)
        `when`(event.x).thenReturn(10)
        `when`(event.y).thenReturn(10)
        `when`(circlePanel.circle).thenReturn(Ellipse2D.Double(0.0, 0.0, 20.0, 20.0))
        `when`(circlePanel.commit).thenReturn(commit1)
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
        `when`(circlePanel.commit).thenReturn(commit1)
        listener.mouseClicked(null)
        assertThat(circlePanel.commit.isSelected).isTrue()
        verify(circlePanel).repaint()
    }

    fun testMouseMovedInsideCircle() {
        val event = mock(MouseEvent::class.java)
        `when`(event.x).thenReturn(10)
        `when`(event.y).thenReturn(10)
        `when`(circlePanel.circle).thenReturn(Ellipse2D.Double(0.0, 0.0, 20.0, 20.0))
        `when`(circlePanel.commit).thenReturn(commit1)
        listener.mouseMoved(event)

        assertThat(circlePanel.commit.isHovered).isTrue()
        verify(circlePanel).repaint()
    }

    fun testMouseMovedOutsideCircle() {
        val event = mock(MouseEvent::class.java)
        `when`(event.x).thenReturn(100)
        `when`(event.y).thenReturn(100)
        `when`(circlePanel.circle).thenReturn(Ellipse2D.Double(10.0, 10.0, 20.0, 20.0))
        `when`(circlePanel.commit).thenReturn(commit1)
        listener.mouseMoved(event)

        assertThat(circlePanel.commit.isHovered).isFalse()
        verify(circlePanel).repaint()
    }
}
