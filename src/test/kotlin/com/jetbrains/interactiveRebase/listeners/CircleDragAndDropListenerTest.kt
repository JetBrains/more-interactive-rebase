package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.ReorderCommand
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import com.jetbrains.interactiveRebase.visuals.BranchPanel
import com.jetbrains.interactiveRebase.visuals.CirclePanel
import com.jetbrains.interactiveRebase.visuals.LabeledBranchPanel
import com.jetbrains.interactiveRebase.visuals.Palette
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.awt.Point
import java.awt.event.ActionEvent
import java.awt.event.MouseEvent
import javax.swing.Timer

class CircleDragAndDropListenerTest : BasePlatformTestCase() {
    private lateinit var circle: CirclePanel
    private lateinit var other: CirclePanel
    private lateinit var circles: MutableList<CirclePanel>
    private lateinit var parent: LabeledBranchPanel
    private lateinit var branch: BranchInfo
    private lateinit var listener: CircleDragAndDropListener
    private lateinit var commit: CommitInfo
    private lateinit var otherCommit: CommitInfo
    private lateinit var messages: MutableList<JBPanel<JBPanel<*>>>
    private lateinit var message1: JBPanel<JBPanel<*>>
    private lateinit var message2: JBPanel<JBPanel<*>>
    private lateinit var invoker: RebaseInvoker

    override fun setUp() {
        super.setUp()

        commit = mock(CommitInfo::class.java)
        otherCommit = mock(CommitInfo::class.java)
        circle = mock(CirclePanel::class.java)
        `when`(circle.commit).thenReturn(commit)
        `when`(circle.centerX).thenReturn(10.0)
        `when`(circle.centerY).thenReturn(30.0)
        `when`(circle.x).thenReturn(10)
        `when`(circle.y).thenReturn(20)
        `when`(circle.height).thenReturn(20)
        other = mock(CirclePanel::class.java)
        `when`(other.commit).thenReturn(otherCommit)
        `when`(other.centerX).thenReturn(10.0)
        `when`(other.centerY).thenReturn(70.0)
        `when`(other.x).thenReturn(10)
        `when`(other.y).thenReturn(60)
        val label = JBLabel()
        label.labelFor = circle
        val otherLabel = JBLabel()
        otherLabel.labelFor = other
        circles = mutableListOf(circle, other)
        branch = mock(BranchInfo()::class.java)
        `when`(branch.currentCommits).thenReturn(mutableListOf(commit, otherCommit))

        message1 =
            mock(JBPanel<JBPanel<*>>()::class.java).apply {
                `when`(this.x).thenReturn(0)
                `when`(this.y).thenReturn(2)
            }

        message2 =
            mock(JBPanel<JBPanel<*>>()::class.java).apply {
                `when`(this.x).thenReturn(0)
                `when`(this.y).thenReturn(6)
            }

        messages = mutableListOf(message1, message2)

        val branchPanel = mock(BranchPanel::class.java)
        `when`(branchPanel.height).thenReturn(200)

        invoker = project.service<RebaseInvoker>()

        parent = mock(LabeledBranchPanel::class.java)
        `when`(parent.project).thenReturn(project)
        `when`(parent.branch).thenReturn(branch)
        `when`(parent.messages).thenReturn(messages)
        `when`(parent.commitLabels).thenReturn(mutableListOf(label, otherLabel))
        `when`(parent.branchPanel).thenReturn(branchPanel)

        listener = spy(CircleDragAndDropListener(project, circle, circles, parent))
    }

    fun testMousePressed() {
        val event =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(100)
                `when`(yOnScreen).thenReturn(100)
            }

        listener.mousePressed(event)

        assertThat(listener.wasDragged).isFalse()
        verify(listener).updateMousePosition(event)
        assertThat(listener.circlesPositions).isEqualTo(
            mutableListOf(
                CirclePosition(10, 30, 10, 20),
                CirclePosition(10, 70, 10, 60),
            ),
        )
        assertThat(listener.messagesPositions).isEqualTo(
            mutableListOf(
                Point(0, 2),
                Point(0, 6),
            ),
        )
    }

    fun testMouseDragged() {
        val eventPress =
            mock(MouseEvent::class.java).apply {
                `when`(yOnScreen).thenReturn(30)
            }
        val eventDrag =
            mock(MouseEvent::class.java).apply {
                `when`(yOnScreen).thenReturn(40)
            }

        listener.mousePressed(eventPress)
        listener.mouseDragged(eventDrag)

        verify(circle.commit).setDraggedTo(true)
        verify(listener).setCurrentCircleLocation(30)
        verify(listener).updateMousePosition(eventDrag)
        verify(listener).findNewBranchIndex()

        verify(listener, never()).updateIndices(1, 0)
        verify(listener, never()).repositionOnDrag()
        assertThat(listener.currentIndex).isEqualTo(0)
        verify(parent, never()).repaint()
        verify(listener).indicateLimitedVerticalMovement(30)
    }

    fun testMousePosition() {
        val event =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(100)
                `when`(yOnScreen).thenReturn(100)
            }

        listener.updateMousePosition(event)

        assertThat(listener.mousePosition).isEqualTo(Point(100, 100))
    }

    fun testMouseReleasedTrue() {
        listener.wasDragged = true

        val eventPress =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(100)
                `when`(yOnScreen).thenReturn(10)
            }
        val eventDrag =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(100)
                `when`(yOnScreen).thenReturn(100)
            }
        val eventRelease =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(100)
                `when`(yOnScreen).thenReturn(150)
            }

        listener.mousePressed(eventPress)
        listener.mouseDragged(eventDrag)
        listener.mouseReleased(eventRelease)
        assertThat(commit.isDragged).isFalse()
        verify(listener).repositionOnDrop()
        verify(listener, never()).markCommitAsReordered()

        verify(parent.branch).updateCurrentCommits(
            listener.initialIndex,
            listener.currentIndex,
            commit,
        )
    }

    fun testMouseReleasedFalse() {
        listener.wasDragged = false
        val eventPress =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(100)
                `when`(yOnScreen).thenReturn(10)
            }
        val eventRelease =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(100)
                `when`(yOnScreen).thenReturn(150)
            }

        listener.mousePressed(eventPress)
        listener.mouseReleased(eventRelease)
        verify(listener, never()).repositionOnDrop()
        verify(listener, never()).markCommitAsReordered()

        verify(parent.branch, never()).updateCurrentCommits(
            listener.initialIndex,
            listener.currentIndex,
            commit,
        )
    }

    fun testMarkCommitAsReordered() {
        listener.markCommitAsReordered()
        verify(commit).setReorderedTo(true)
        verify(commit).addChange(ReorderCommand(1, 1))
        assertThat(invoker.commands.any { it is ReorderCommand }).isTrue()
    }

    fun testSetCurrentCircleLocation() {
        listener.setCurrentCircleLocation(300)
        verify(circle).setLocation(10, 200)
        verify(listener.message).setLocation(0, 200)
    }

    fun testUpdateIndices() {
        listener.updateIndices(1, 0)

        assertThat(listener.circles).isEqualTo(mutableListOf(other, circle))
        assertThat(listener.messages).isEqualTo(mutableListOf(message2, message1))
        verify(listener, times(1)).updateNeighbors(0)
        verify(listener, times(1)).updateNeighbors(1)
    }

    fun testUpdateNeighbors() {
        val circle0 = CirclePanel(20.0, 1f, Palette.BLUE, commit)
        val circle1 = CirclePanel(20.0, 1f, Palette.BLUE, commit)
        val circle2 = CirclePanel(20.0, 1f, Palette.BLUE, commit)
        listener =
            spy(
                CircleDragAndDropListener(
                    project,
                    circle,
                    mutableListOf(circle0, circle1, circle2),
                    parent,
                ),
            )
        listener.updateNeighbors(1)

        assertThat(circle1.next).isEqualTo(circle2)
        assertThat(circle1.previous).isEqualTo(circle0)

        listener.updateNeighbors(2)

        assertThat(circle2.next).isNull()
        assertThat(circle2.previous).isEqualTo(circle1)

        listener.updateNeighbors(0)

        assertThat(circle0.previous).isNull()
        assertThat(circle0.next).isEqualTo(circle1)
    }

    fun testFindNewBranchIndex() {
        listener.circlesPositions =
            mutableListOf(
                CirclePosition(10, 10, 0, 0),
                CirclePosition(10, 20, 0, 10),
                CirclePosition(10, 30, 0, 20),
                CirclePosition(10, 40, 0, 30),
            )
        `when`(circle.y).thenReturn(19)
        assertThat(listener.findNewBranchIndex())

        `when`(circle.y).thenReturn(49)
        assertThat(listener.findNewBranchIndex())
            .isEqualTo(3)
    }

    fun testRepositionOnDrop() {
        val circle0 = CirclePanel(20.0, 1f, Palette.BLUE, commit)
        val circle1 = CirclePanel(20.0, 1f, Palette.BLUE, commit)
        val circle2 = CirclePanel(20.0, 1f, Palette.BLUE, commit)
        val circle3 = CirclePanel(20.0, 1f, Palette.BLUE, commit)
        listener =
            spy(
                CircleDragAndDropListener(
                    project,
                    circle,
                    mutableListOf(circle0, circle1, circle2, circle3),
                    parent,
                ),
            )
        listener.circlesPositions =
            mutableListOf(
                CirclePosition(10, 10, 0, 0),
                CirclePosition(10, 20, 0, 10),
                CirclePosition(10, 30, 0, 20),
                CirclePosition(10, 40, 0, 30),
            )

        listener.messages =
            mutableListOf(
                JBPanel(),
                JBPanel(),
                JBPanel(),
                JBPanel(),
            )

        listener.messagesPositions =
            mutableListOf(
                Point(0, 0),
                Point(0, 10),
                Point(0, 20),
                Point(0, 30),
            )

        listener.repositionOnDrop()

        assertThat(circle0.location).isEqualTo(Point(0, 0))
        assertThat(circle1.location).isEqualTo(Point(0, 10))
        assertThat(circle2.location).isEqualTo(Point(0, 20))
        assertThat(circle3.location).isEqualTo(Point(0, 30))

        assertThat(listener.messages[0].location).isEqualTo(Point(0, 0))
        assertThat(listener.messages[1].location).isEqualTo(Point(0, 10))
        assertThat(listener.messages[2].location).isEqualTo(Point(0, 20))
        assertThat(listener.messages[3].location).isEqualTo(Point(0, 30))
    }

    fun testRepositionOnDrag() {
        listener.circlesPositions =
            mutableListOf(
                CirclePosition(10, 10, 10, 30),
                CirclePosition(10, 20, 10, 70),
            )

        listener.repositionOnDrag()

        verify(listener).calculateStepSizes(
            mutableListOf(
                Point(10, 20),
                Point(10, 60),
            ),
            mutableListOf(
                Point(10, 30),
                Point(10, 70),
            ),
        )

        verify(listener).createReorderAnimation(
            mutableListOf(
                Point(0, 1),
                Point(0, 1),
            ),
            mutableListOf(
                Point(10, 30),
                Point(10, 70),
            ),
        )
    }

    fun testStartReorderAnimation() {
        val timer = Timer(3) {}
        listener.startReorderAnimation(timer)
        assertThat(timer.initialDelay).isZero()
        assertThat(timer.isRepeats).isTrue()
    }

    fun testStopAnimationIfComplete() {
        val timer = mock(Timer::class.java)
        val action = mock(ActionEvent::class.java)
        `when`(action.source).thenReturn(timer)
        listener.stopAnimationIfComplete(10, 10, action)
        verify(action.source as Timer).stop()
    }

    fun testStopAnimationIfIncomplete() {
        val timer = mock(Timer::class.java)
        val action = mock(ActionEvent::class.java)
        `when`(action.source).thenReturn(timer)
        listener.stopAnimationIfComplete(8, 10, action)
        verify(action.source as Timer, never()).stop()
    }

    fun testReorderStep() {
        assertThat(listener.reorderStep(70, 60, 9)).isEqualTo(69)
        assertThat(listener.reorderStep(70, 60, 11)).isEqualTo(70)
    }

    fun testGetLabelIndex() {
        assertThat(listener.getLabelIndex(circle, parent.commitLabels)).isEqualTo(0)
    }

    fun testMoveAllCircles() {
        listener.moveAllCircles(20)

        assertThat(listener.circles[0].x).isEqualTo(10)
        assertThat(listener.circles[1].x).isEqualTo(10)
        assertThat(listener.messages[0].x).isEqualTo(0)
        assertThat(listener.messages[1].x).isEqualTo(0)
    }

    fun testIndicateLimitedVerticalMovement() {
        listener.indicateLimitedVerticalMovement(100)
        verify(listener, never()).moveAllCircles(anyInt())

        listener.indicateLimitedVerticalMovement(300)
        verify(listener).moveAllCircles(-100)

        listener.indicateLimitedVerticalMovement(-100)
        verify(listener).moveAllCircles(100)
    }
}
