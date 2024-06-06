package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.application.EDT
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.visuals.CirclePanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doAnswer
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

    init {
        System.setProperty("idea.home.path", "/tmp")
    }

    override fun setUp() {
        super.setUp()
        circlePanel = mock(CirclePanel::class.java)
        val commitProvider = TestGitCommitProvider(project)
        commit1 = CommitInfo(commitProvider.createCommit("tests"), project, mutableListOf())

        val commitService = mock(CommitService::class.java)

        doAnswer {
            listOf(commit1.commit)
        }.`when`(commitService).getCommits(anyString())

        doAnswer {
            "feature1"
        }.`when`(commitService).getBranchName()

        val modelService = ModelService(project, CoroutineScope(Dispatchers.EDT), commitService)
        listener = CircleHoverListener(circlePanel)
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

//    fun testMouseClicked() {
//        val event = mock(MouseEvent::class.java)
//        `when`(circlePanel.commit).thenReturn(commit1)
//        `when`(event.isShiftDown).thenReturn(false)
//        `when`(event.isControlDown).thenReturn(false)
//
//        listener.mouseClicked(event)
//        assertThat(circlePanel.commit.isSelected).isTrue()
//    }

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
