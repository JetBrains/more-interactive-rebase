package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.labels.BoldLabel
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.GraphInfo
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.visuals.*
import com.jetbrains.interactiveRebase.visuals.multipleBranches.RoundedPanel
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.*
import java.awt.Dimension
import java.awt.GridBagLayout
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseEvent
import javax.swing.border.EmptyBorder

class RebaseDragAndDropListenerTest : BasePlatformTestCase() {
    private lateinit var listener: RebaseDragAndDropListener
    private lateinit var graphPanel: GraphPanel
    private lateinit var dragPanel: DragPanel
    private lateinit var mainBranchPanel: LabeledBranchPanel
    private lateinit var addedBranchPanel: LabeledBranchPanel
    private lateinit var mainBranchNameLabel: JBPanel<*>
    private lateinit var addedBranchNameLabel: JBPanel<*>
    private lateinit var mainBranchInfo: BranchInfo
    private lateinit var branchPanel: BranchPanel
    private lateinit var helpMsg: JBPanel<*>

    override fun setUp() {
        super.setUp()
        val mainPanel = mock(MainPanel::class.java)
        dragPanel = mock(DragPanel::class.java)
        project.service<ActionService>().mainPanel = mainPanel
        `when`(mainPanel.dragPanel).thenReturn(dragPanel)

        val label1 = BoldLabel("Branch 1")
        helpMsg = mock(JBPanel::class.java)
        `when`(helpMsg.getComponent(0)).thenReturn(mock(JBPanel::class.java))
        `when`(helpMsg.getComponent(1)).thenReturn(mock(JBLabel::class.java))
        `when`(helpMsg.components).thenReturn(
            arrayOf(
                mock(JBPanel::class.java), mock(JBLabel::class.java))
        )
        val roundedPanel1 = mock(RoundedPanel::class.java)
        `when`(roundedPanel1.cornerRadius).thenReturn(15)
        `when`(roundedPanel1.getComponent(0)).thenReturn(label1)
        mainBranchNameLabel = mock(JBPanel::class.java)
        `when`(mainBranchNameLabel.x).thenReturn(10)
        `when`(mainBranchNameLabel.y).thenReturn(20)
        `when`(mainBranchNameLabel.width).thenReturn(40)
        `when`(mainBranchNameLabel.height).thenReturn(10)
        `when`(mainBranchNameLabel.bounds).thenReturn(Rectangle(Point(10, 20), Dimension(40, 10)))
        `when`(mainBranchNameLabel.border).thenReturn(EmptyBorder(2, 3, 3, 3))
        `when`(mainBranchNameLabel.getComponent(0)).thenReturn(roundedPanel1)
        `when`(mainBranchNameLabel.getComponent(1)).thenReturn(helpMsg)
        `when`(mainBranchNameLabel.getComponent(1)).thenReturn(helpMsg)
        `when`(mainBranchNameLabel.components).thenReturn(
            arrayOf(roundedPanel1, helpMsg)
        )
        `when`(dragPanel.add(mainBranchNameLabel)).thenReturn(mainBranchNameLabel)
        `when`(dragPanel.width).thenReturn(200)
        `when`(dragPanel.height).thenReturn(200)

        val label2 = BoldLabel("Branch 2")
        val roundedPanel2 = mock(RoundedPanel::class.java)
        `when`(roundedPanel2.cornerRadius).thenReturn(15)
        `when`(roundedPanel2.getComponent(0)).thenReturn(label2)
        addedBranchNameLabel = mock(JBPanel::class.java)
        `when`(addedBranchNameLabel.x).thenReturn(110)
        `when`(addedBranchNameLabel.y).thenReturn(20)
        `when`(addedBranchNameLabel.width).thenReturn(40)
        `when`(addedBranchNameLabel.height).thenReturn(10)
        `when`(addedBranchNameLabel.bounds).thenReturn(Rectangle(Point(110, 20), Dimension(40, 10)))
        `when`(addedBranchNameLabel.border).thenReturn(EmptyBorder(2, 3, 3, 3))
        `when`(addedBranchNameLabel.getComponent(0)).thenReturn(roundedPanel2)
        `when`(addedBranchNameLabel.getComponent(1)).thenReturn(null)
        `when`(addedBranchNameLabel.components).thenReturn(
            arrayOf(roundedPanel2)
        )
        `when`(dragPanel.add(addedBranchNameLabel)).thenReturn(addedBranchNameLabel)

        mainBranchInfo = mock(BranchInfo::class.java)
        `when`(mainBranchInfo.isRebased).thenReturn(true)
        mainBranchPanel = mock(LabeledBranchPanel::class.java)
        `when`(mainBranchPanel.x).thenReturn(0)
        `when`(mainBranchPanel.y).thenReturn(10)
        `when`(mainBranchPanel.colorTheme).thenReturn(Palette.BLUE_THEME)
        `when`(mainBranchPanel.branchNamePanel).thenReturn(mainBranchNameLabel)
        `when`(mainBranchPanel.instantiateBranchNamePanel()).thenReturn(mainBranchNameLabel)
        `when`(mainBranchPanel.layout).thenReturn(GridBagLayout())
        `when`(mainBranchPanel.branch).thenReturn(mainBranchInfo)
        `when`(mainBranchNameLabel.parent).thenReturn(mainBranchPanel)
        addedBranchPanel = mock(LabeledBranchPanel::class.java)
        `when`(addedBranchPanel.x).thenReturn(100)
        `when`(addedBranchPanel.y).thenReturn(110)
        `when`(addedBranchPanel.colorTheme).thenReturn(Palette.TOMATO_THEME)
        `when`(addedBranchPanel.branchNamePanel).thenReturn(addedBranchNameLabel)
        `when`(addedBranchPanel.instantiateBranchNamePanel()).thenReturn(addedBranchNameLabel)
        `when`(addedBranchPanel.layout).thenReturn(GridBagLayout())
        `when`(addedBranchNameLabel.parent).thenReturn(addedBranchPanel)

        branchPanel = mock(BranchPanel::class.java)
        `when`(branchPanel.circles).thenReturn(
            mutableListOf(
                mock(CirclePanel::class.java),
                mock(CirclePanel::class.java),
            ),
        )
        `when`(addedBranchPanel.branchPanel).thenReturn(branchPanel)
        graphPanel = mock(GraphPanel::class.java)
        `when`(graphPanel.computeVerticalOffsets()).thenReturn(Pair(5, 5))
        graphPanel.mainBranchPanel = mainBranchPanel
        graphPanel.addedBranchPanel = addedBranchPanel

        val addedBranch = mock(BranchInfo::class.java)
        `when`(addedBranch.baseCommit).thenReturn(mock(CommitInfo::class.java))
        `when`(addedBranch.currentCommits).thenReturn(mutableListOf(mock(CommitInfo::class.java)))
        val graphInfo = mock(GraphInfo::class.java)
        `when`(graphInfo.addedBranch).thenReturn(addedBranch)
        `when`(graphInfo.mainBranch).thenReturn(mainBranchInfo)

        `when`(graphPanel.graphInfo).thenReturn(graphInfo)

        listener =
            spy(
                RebaseDragAndDropListener(
                    project,
                    mainBranchNameLabel,
                    addedBranchNameLabel,
                    graphPanel,
                ),
            )
    }

    fun testMousePressed() {
        val event =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(100)
                `when`(yOnScreen).thenReturn(100)
            }
        listener.mousePressed(event)
        assertThat(listener.initialPositionMain).isEqualTo(Point(10, 30))
        assertThat(listener.initialPositionAdded).isEqualTo(Point(210, 130))
    }

    fun testMouseDragged() {
        val eventPress =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(20)
                `when`(yOnScreen).thenReturn(30)
            }
        val eventDrag =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(30)
                `when`(yOnScreen).thenReturn(40)
            }

        listener.mousePressed(eventPress)
        listener.mouseDragged(eventDrag)

        verify(listener).addLabelsToDragPanel()
        verify(listener).substituteLabelForPlaceholderMainBranch()
        verify(listener).substituteLabelForPlaceholderAddedBranch()
        verify(listener, times(2)).formatDraggedLabelOnDrag()
        verify(listener).setBranchNameLocation(eventDrag)
        verify(listener).updateMousePosition(eventDrag)

        verify(listener).indicateDraggedLabelCanBeDroppedOnTheSecondLabel()

        verify(listener).renderCurvedArrow()
    }

    fun testMouseReleased() {
        val eventPress =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(10)
                `when`(yOnScreen).thenReturn(20)
            }
        val eventDrag =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(20)
                `when`(yOnScreen).thenReturn(30)
            }
        val eventRelease =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(111)
                `when`(yOnScreen).thenReturn(25)
            }

        `when`(mainBranchNameLabel.x).thenReturn(111)
        `when`(mainBranchNameLabel.y).thenReturn(25)
        `when`(mainBranchNameLabel.width).thenReturn(40)
        `when`(mainBranchNameLabel.height).thenReturn(10)
        `when`(mainBranchNameLabel.bounds).thenReturn(Rectangle(Point(111, 25), Dimension(40, 10)))

        `when`(mainBranchPanel.branchNamePanel).thenReturn(mainBranchNameLabel)

        listener.mousePressed(eventPress)
        listener.mouseDragged(eventDrag)
        listener.mouseReleased(eventRelease)

        verify(listener, times(1)).resetFormattingOfSecondLabel()
        verify(listener, times(2)).formatDraggedLabelOnDrop()
        verify(dragPanel, times(4)).repaint()
        verify(listener).rebase(graphPanel.graphInfo.addedBranch!!.currentCommits[0])
        verify(listener).returnNameLabelsBackInGraph()
        verify(listener, times(2)).refreshDraggableArea()
    }

    fun testMouseReleasedWithoutRebase() {
        val eventPress =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(11)
                `when`(yOnScreen).thenReturn(21)
            }
        val eventDrag =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(20)
                `when`(yOnScreen).thenReturn(30)
            }
        val eventRelease =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(111)
                `when`(yOnScreen).thenReturn(25)
            }

        listener.mousePressed(eventPress)
        listener.mouseDragged(eventDrag)
        listener.mouseReleased(eventRelease)

        verify(listener, times(2)).resetFormattingOfSecondLabel()
        verify(listener).formatDraggedLabelOnDrop()
        verify(dragPanel, times(4)).repaint()
        verify(listener).formatDraggedLabelOnDrop()
        verify(listener, never()).rebase(graphPanel.graphInfo.addedBranch!!.currentCommits[0])
        verify(listener).returnNameLabelsBackInGraph()
        verify(listener, times(2)).refreshDraggableArea()
    }

    fun testMouseReleasedSize1() {
        val eventPress =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(11)
                `when`(yOnScreen).thenReturn(21)
            }
        val eventDrag =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(20)
                `when`(yOnScreen).thenReturn(30)
            }
        val eventRelease =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(111)
                `when`(yOnScreen).thenReturn(25)
            }

        `when`(branchPanel.circles).thenReturn(
            mutableListOf(mock(CirclePanel::class.java)),
        )

        listener.mousePressed(eventPress)
        listener.mouseDragged(eventDrag)
        listener.mouseReleased(eventRelease)

        verify(listener, times(2)).resetFormattingOfSecondLabel()
        verify(listener).formatDraggedLabelOnDrop()
        verify(dragPanel, times(4)).repaint()
        verify(listener).formatDraggedLabelOnDrop()
        verify(listener, never()).rebase(graphPanel.graphInfo.addedBranch!!.currentCommits[0])
        verify(listener).returnNameLabelsBackInGraph()
        verify(listener, times(2)).refreshDraggableArea()
    }

    fun testMouseReleasedIntersectionSize1() {
        val eventPress =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(11)
                `when`(yOnScreen).thenReturn(21)
            }
        val eventDrag =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(20)
                `when`(yOnScreen).thenReturn(30)
            }
        val eventRelease =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(111)
                `when`(yOnScreen).thenReturn(25)
            }

        `when`(mainBranchNameLabel.x).thenReturn(111)
        `when`(mainBranchNameLabel.y).thenReturn(25)
        `when`(mainBranchNameLabel.width).thenReturn(40)
        `when`(mainBranchNameLabel.height).thenReturn(10)
        `when`(mainBranchNameLabel.bounds).thenReturn(Rectangle(Point(111, 25), Dimension(40, 10)))

        `when`(mainBranchPanel.branchNamePanel).thenReturn(mainBranchNameLabel)
        `when`(branchPanel.circles).thenReturn(
            mutableListOf(mock(CirclePanel::class.java)),
        )

        listener.mousePressed(eventPress)
        listener.mouseDragged(eventDrag)
        listener.mouseReleased(eventRelease)

        verify(listener, times(1)).resetFormattingOfSecondLabel()
        verify(listener).formatDraggedLabelOnDrop()
        verify(dragPanel, times(4)).repaint()
        verify(listener).formatDraggedLabelOnDrop()
        verify(listener, never()).rebase(graphPanel.graphInfo.addedBranch!!.currentCommits[0])
        verify(listener).returnNameLabelsBackInGraph()
        verify(listener, times(2)).refreshDraggableArea()
    }

    fun testMouseReleasedIsNotRebased() {
        val eventPress =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(11)
                `when`(yOnScreen).thenReturn(21)
            }
        val eventDrag =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(20)
                `when`(yOnScreen).thenReturn(30)
            }
        val eventRelease =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(111)
                `when`(yOnScreen).thenReturn(25)
            }

        `when`(mainBranchInfo.isRebased).thenReturn(false)

        listener.mousePressed(eventPress)
        listener.mouseDragged(eventDrag)
        listener.mouseReleased(eventRelease)

        verify(listener, times(2)).resetFormattingOfSecondLabel()
        verify(listener, never()).formatDraggedLabelOnDrop()
        verify(dragPanel, times(4)).repaint()
        verify(listener, never()).rebase(graphPanel.graphInfo.addedBranch!!.currentCommits[0])
        verify(listener).returnNameLabelsBackInGraph()
        verify(listener, times(2)).refreshDraggableArea()
    }

    fun testIndicateDraggedLabelCanBeDroppedOnTheSecondLabel() {
        `when`(mainBranchNameLabel.x).thenReturn(111)
        `when`(mainBranchNameLabel.y).thenReturn(25)
        `when`(mainBranchNameLabel.width).thenReturn(40)
        `when`(mainBranchNameLabel.height).thenReturn(10)
        `when`(mainBranchNameLabel.bounds).thenReturn(Rectangle(Point(111, 25), Dimension(40, 10)))

        listener.indicateDraggedLabelCanBeDroppedOnTheSecondLabel()

        verify(listener).changeFormattingOfSecondLabelWhenUserCanDropOnIt()
    }

    fun testIndicateDraggedLabelCanBeDroppedOnTheSecondLabelNoIntersection() {
        listener.indicateDraggedLabelCanBeDroppedOnTheSecondLabel()

        verify(listener).resetFormattingOfSecondLabel()
    }

    fun testChangeFormattingOfSecondLabelWhenUserCanDropOnIt() {
        listener.changeFormattingOfSecondLabelWhenUserCanDropOnIt()
        verify(addedBranchNameLabel, times(7)).getComponent(0)
        verify(addedBranchNameLabel).repaint()
    }

    fun testRebase() {
        listener.rebase(graphPanel.graphInfo.addedBranch!!.currentCommits[0])
        verify(graphPanel, times(2)).computeVerticalOffsets()

        verify(listener).animateAndPropagateToBackend(
            5,
            5,
            5,
            5,
        )
    }

    fun testAnimateAndPropagateToBackend() {
        listener.animateAndPropagateToBackend(
            100,
            5,
            5,
            100,
        )

        listener.animateAndPropagateToBackend(
            5,
            5,
            5,
            10,
            350,
            7,
        )
    }
}
