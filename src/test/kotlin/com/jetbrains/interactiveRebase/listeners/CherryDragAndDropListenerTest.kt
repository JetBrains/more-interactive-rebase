package com.jetbrains.interactiveRebase.listeners

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.GraphInfo
import com.jetbrains.interactiveRebase.visuals.BranchPanel
import com.jetbrains.interactiveRebase.visuals.CirclePanel
import com.jetbrains.interactiveRebase.visuals.DragPanel
import com.jetbrains.interactiveRebase.visuals.GraphPanel
import com.jetbrains.interactiveRebase.visuals.LabeledBranchPanel
import com.jetbrains.interactiveRebase.visuals.MainPanel
import com.jetbrains.interactiveRebase.visuals.Palette
import git4idea.GitCommit
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.awt.Cursor
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseEvent

class CherryDragAndDropListenerTest : BasePlatformTestCase() {
    private lateinit var listener: CherryDragAndDropListener
    private lateinit var cherry: CirclePanel
    private lateinit var clone: CirclePanel
    private lateinit var commit: CommitInfo
    private lateinit var addedBranchPanel: LabeledBranchPanel
    private lateinit var mainBranchPanel: LabeledBranchPanel
    private lateinit var branchPanel1: BranchPanel
    private lateinit var branchPanel2: BranchPanel
    private lateinit var dragPanel: DragPanel
    private lateinit var graphPanel: GraphPanel
    private lateinit var mainBranchInfo: BranchInfo
    private lateinit var addedBranchInfo: BranchInfo
    private lateinit var graphInfo: GraphInfo
    private lateinit var messages: MutableList<JBPanel<JBPanel<*>>>
    private lateinit var message1: JBPanel<JBPanel<*>>
    private lateinit var message2: JBPanel<JBPanel<*>>
    private lateinit var message3: JBPanel<JBPanel<*>>
    private lateinit var labelPanelWrapper: JBPanel<JBPanel<*>>
    private lateinit var mainPanel: MainPanel

    override fun setUp() {
        super.setUp()
        mainPanel = mock(MainPanel::class.java)
        dragPanel = mock(DragPanel::class.java)
        `when`(dragPanel.width).thenReturn(800)
        `when`(dragPanel.height).thenReturn(600)

        commit = mock(CommitInfo::class.java)
        `when`(commit.commit).thenReturn(mock(GitCommit::class.java))
        clone = mock(CirclePanel::class.java)
        `when`(clone.commit).thenReturn(commit)
        cherry = mock(CirclePanel::class.java)
        `when`(cherry.commit).thenReturn(commit)
        `when`(cherry.centerX).thenReturn(40.0)
        `when`(cherry.centerY).thenReturn(40.0)
        `when`(cherry.x).thenReturn(10)
        `when`(cherry.y).thenReturn(10)
        `when`(cherry.height).thenReturn(60)
        `when`(cherry.width).thenReturn(60)
        `when`(cherry.bounds).thenReturn(
            Rectangle(0, 0, 60, 60),
        )
        `when`(cherry.clone()).thenReturn(clone)
        `when`(cherry.minimumSize).thenReturn(Dimension(60, 60))

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

        message3 =
            mock(JBPanel<JBPanel<*>>()::class.java).apply {
                `when`(this.x).thenReturn(0)
                `when`(this.y).thenReturn(10)
            }

        messages = mutableListOf(message1, message2, message3)

        labelPanelWrapper = mock(JBPanel<JBPanel<*>>()::class.java)
        `when`(labelPanelWrapper.layout).thenReturn(GridBagLayout())

        val branchInfo = mock(BranchInfo::class.java)
        `when`(branchInfo.currentCommits).thenReturn(
            mutableListOf(
                mock(CommitInfo::class.java),
                mock(CommitInfo::class.java),
                mock(CommitInfo::class.java),
            ),
        )

        val circle1 = mock(CirclePanel::class.java)
        `when`(circle1.y).thenReturn(5)

        val circle2 = mock(CirclePanel::class.java)
        `when`(circle2.y).thenReturn(50)

        branchPanel1 = mock(BranchPanel::class.java)
        `when`(branchPanel1.circles).thenReturn(
            mutableListOf(
                circle1,
                cherry,
                circle2,
            ),
        )
        `when`(branchPanel1.layout).thenReturn(GridBagLayout())
        `when`(branchPanel1.x).thenReturn(10)
        `when`(branchPanel1.y).thenReturn(10)
        `when`(branchPanel1.branch).thenReturn(branchInfo)

        branchPanel2 = mock(BranchPanel::class.java)
        `when`(branchPanel2.circles).thenReturn(
            mutableListOf(
                circle1,
                cherry,
                circle2,
            ),
        )
        `when`(branchPanel2.layout).thenReturn(GridBagLayout())
        `when`(branchPanel2.x).thenReturn(10)
        `when`(branchPanel2.y).thenReturn(10)
        `when`(branchPanel2.diameter).thenReturn(30)
        `when`(branchPanel2.bounds).thenReturn(
            Rectangle(
                10,
                10,
                200,
                300,
            ),
        )

        mainBranchInfo = mock(BranchInfo::class.java)
        `when`(mainBranchInfo.currentCommits).thenReturn(
            mutableListOf(
                mock(CommitInfo::class.java),
                mock(CommitInfo::class.java),
                mock(CommitInfo::class.java),
            ),
        )
        addedBranchInfo = mock(BranchInfo::class.java)

        mainBranchPanel = mock(LabeledBranchPanel::class.java)
        `when`(mainBranchPanel.x).thenReturn(0)
        `when`(mainBranchPanel.y).thenReturn(0)
        `when`(mainBranchPanel.colorTheme).thenReturn(Palette.BLUE_THEME)
        `when`(mainBranchPanel.layout).thenReturn(GridBagLayout())
        `when`(mainBranchPanel.branchPanel).thenReturn(branchPanel2)
        `when`(mainBranchPanel.branch).thenReturn(mainBranchInfo)
        `when`(mainBranchPanel.messages).thenReturn(messages)
        `when`(mainBranchPanel.labelPanelWrapper).thenReturn(labelPanelWrapper)

        addedBranchPanel = mock(LabeledBranchPanel::class.java)
        `when`(addedBranchPanel.x).thenReturn(100)
        `when`(addedBranchPanel.y).thenReturn(110)
        `when`(addedBranchPanel.colorTheme).thenReturn(Palette.TOMATO_THEME)
        `when`(addedBranchPanel.layout).thenReturn(GridBagLayout())
        `when`(addedBranchPanel.branchPanel).thenReturn(branchPanel1)

        graphInfo = mock(GraphInfo::class.java)
        `when`(graphInfo.mainBranch).thenReturn(mainBranchInfo)
        `when`(graphInfo.addedBranch).thenReturn(addedBranchInfo)

        graphPanel = mock(GraphPanel::class.java)
        `when`(graphPanel.layout).thenReturn(GridBagLayout())
        `when`(graphPanel.lineOffset).thenReturn(60)
        `when`(graphPanel.graphInfo).thenReturn(graphInfo)
        `when`(graphPanel.mainBranchPanel).thenReturn(mainBranchPanel)
        `when`(graphPanel.addedBranchPanel).thenReturn(addedBranchPanel)
        doNothing().`when`(graphPanel).updateGraphPanel()
        `when`(mainPanel.dragPanel).thenReturn(dragPanel)
        `when`(mainPanel.graphPanel).thenReturn(graphPanel)

        listener =
            spy(
                CherryDragAndDropListener(
                    project,
                    cherry,
                    addedBranchPanel,
                    mainPanel,
                ),
            )
    }

    fun testMousePressed() {
        val event =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(20)
                `when`(yOnScreen).thenReturn(30)
            }

        listener.mousePressed(event)

        verify(listener).initializeLateFields()
        assertEquals(Point(20, 30), listener.mousePosition)
        assertEquals(Point(120, 130), listener.initialPositionCherry)
        assertEquals(
            mutableListOf(
                CirclePosition(centerX = 0, centerY = 0, x = 0, y = 5),
                CirclePosition(centerX = 40, centerY = 40, x = 10, y = 10),
                CirclePosition(centerX = 0, centerY = 0, x = 0, y = 50),
            ),
            listener.circlesPositions,
        )

        assertFalse(listener.wasHoveringOnMainBranch)
        assertFalse(listener.wasDragged)
    }

    fun testMouseDragged() {
        val eventPress =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(120)
                `when`(yOnScreen).thenReturn(130)
            }
        val eventDrag =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(330)
                `when`(yOnScreen).thenReturn(340)
            }

        listener.mousePressed(eventPress)

        `when`(cherry.x).thenReturn(330)
        `when`(cherry.y).thenReturn(340)
        `when`(cherry.width).thenReturn(60)
        `when`(cherry.height).thenReturn(60)
        `when`(cherry.bounds).thenReturn(
            Rectangle(Point(330, 340), Dimension(60, 60)),
        )

        listener.mouseDragged(eventDrag)
        verify(listener).setUpDrag()
        verify(dragPanel).cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
        verify(listener).setCherryLocation(eventDrag)
        println(cherry.bounds)
        println(branchPanel2.bounds)
        assertFalse(listener.isOverMainBranch())
        verify(listener, never()).makeSpaceForCherryOnMainBranch()
        verify(listener).turnBackToTheInitialLayoutOfMainBranch()
    }

    fun testMouseDraggedAlreadyHovering() {
        val eventPress =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(120)
                `when`(yOnScreen).thenReturn(130)
            }
        val eventDrag =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(330)
                `when`(yOnScreen).thenReturn(340)
            }

        listener.mousePressed(eventPress)

        `when`(cherry.x).thenReturn(330)
        `when`(cherry.y).thenReturn(340)
        `when`(cherry.width).thenReturn(60)
        `when`(cherry.height).thenReturn(60)
        `when`(cherry.bounds).thenReturn(
            Rectangle(Point(330, 340), Dimension(60, 60)),
        )

        listener.animationInProgress = true
        listener.wasHoveringOnMainBranch = true
        listener.mouseDragged(eventDrag)
        verify(listener).setUpDrag()
        verify(dragPanel).cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
        verify(listener).setCherryLocation(eventDrag)
        assertFalse(listener.isOverMainBranch())
        assertFalse(listener.wasHoveringOnMainBranch)
        verify(listener, never()).makeSpaceForCherryOnMainBranch()
        verify(listener).turnBackToTheInitialLayoutOfMainBranch()
        verify(listener).getConstraintsForReset()
    }

    fun testMouseDraggedAlreadyWithSetUp() {
        val eventPress =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(20)
                `when`(yOnScreen).thenReturn(30)
            }
        val eventDrag =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(110)
                `when`(yOnScreen).thenReturn(120)
            }

        listener.mousePressed(eventPress)

        listener.wasDragged = true

        listener.mouseDragged(eventDrag)
        verify(listener, never()).setUpDrag()
        verify(dragPanel).cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
        verify(listener).setCherryLocation(eventDrag)
        assertTrue(listener.isOverMainBranch())
        verify(listener).makeSpaceForCherryOnMainBranch()
        verify(listener, never()).turnBackToTheInitialLayoutOfMainBranch()
    }

    fun testMouseReleasedNotDragged() {
        val eventPress =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(10)
                `when`(yOnScreen).thenReturn(20)
            }
        val eventRelease =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(111)
                `when`(yOnScreen).thenReturn(25)
            }

        listener.mousePressed(eventPress)
        listener.mouseReleased(eventRelease)

        verify(listener, never()).propagateCherryPickToBackend()
        verify(commit, never()).wasCherryPicked = true
        verify(commit, never()).wasCherryPicked = false
        verify(graphPanel, times(1)).lineOffset
        verify(graphPanel, never()).updateGraphPanel()
        verify(branchPanel1, never()).repaint()
    }

    fun testMouseReleasedNoCherryPick() {
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

        listener.mousePressed(eventPress)

        `when`(cherry.x).thenReturn(330)
        `when`(cherry.y).thenReturn(340)
        `when`(cherry.width).thenReturn(60)
        `when`(cherry.height).thenReturn(60)
        `when`(cherry.bounds).thenReturn(
            Rectangle(Point(330, 340), Dimension(60, 60)),
        )

        listener.mouseDragged(eventDrag)
        listener.mouseReleased(eventRelease)

        assertFalse(listener.isOverMainBranch())
        verify(listener, never()).propagateCherryPickToBackend()
        verify(commit, times(2)).wasCherryPicked = false
        verify(commit).isHovered = false
        verify(graphPanel).lineOffset = 60
        verify(graphPanel).updateGraphPanel()
        verify(branchPanel1).repaint()
    }

    fun testMouseReleasedNull() {
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
        val eventRelease = null

        listener.mousePressed(eventPress)

        `when`(cherry.x).thenReturn(330)
        `when`(cherry.y).thenReturn(340)
        `when`(cherry.width).thenReturn(60)
        `when`(cherry.height).thenReturn(60)
        `when`(cherry.bounds).thenReturn(
            Rectangle(Point(330, 340), Dimension(60, 60)),
        )

        listener.mouseDragged(eventDrag)
        listener.mouseReleased(eventRelease)

        assertFalse(listener.isOverMainBranch())
        verify(listener, never()).propagateCherryPickToBackend()
        verify(commit, times(2)).wasCherryPicked = false
        verify(clone, times(2)).commit
        verify(commit).isHovered = false
        verify(graphPanel).lineOffset = 60
        verify(graphPanel).updateGraphPanel()
        verify(branchPanel1).repaint()
    }

    fun testComputeFinalLineOffset() {
        listener.mainCircles = mutableListOf(mock(CirclePanel::class.java))
        listener.mainIndex = 1

        assertEquals(
            90,
            listener.computeFinalLineOffset(),
        )

        listener.mainCircles = mutableListOf(mock(CirclePanel::class.java))
        listener.mainIndex = 0

        assertEquals(
            30,
            listener.computeFinalLineOffset(),
        )
    }

    fun testModifyInsetsToMakeSpaceForCherry() {
        listener.mainIndex = 6
        val gbc = GridBagConstraints()
        listener.modifyInsetsToMakeSpaceForCherry(gbc, 5)
        assertEquals(60, gbc.insets.bottom)
    }

    fun testModifyInsetsToMakeSpaceForCherry2() {
        listener.mainIndex = 5
        val gbc = GridBagConstraints()
        listener.modifyInsetsToMakeSpaceForCherry(gbc, 5)
        assertEquals(0, gbc.insets.bottom)
    }

    fun testModifyInsetsToMakeSpaceForCherry3() {
        val event =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(20)
                `when`(yOnScreen).thenReturn(30)
            }

        listener.mousePressed(event)
        listener.mainIndex = 0
        val gbc = GridBagConstraints()
        listener.modifyInsetsToMakeSpaceForCherry(gbc, 0)
        assertEquals(90, gbc.insets.top)
    }

    fun testModifyInsetsToMakeSpaceForCherry4() {
        val event =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(20)
                `when`(yOnScreen).thenReturn(30)
            }

        listener.mousePressed(event)
        listener.mainIndex = 5
        val gbc = GridBagConstraints()
        listener.modifyInsetsToMakeSpaceForCherry(gbc, 0)
        assertEquals(30, gbc.insets.top)
    }

    fun testModifyInsetsToMakeSpaceForCherry5() {
        val event =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(20)
                `when`(yOnScreen).thenReturn(30)
            }

        listener.mousePressed(event)
        listener.mainIndex = 0
        val gbc = GridBagConstraints()
        listener.modifyInsetsToMakeSpaceForCherry(gbc, 5)
        assertEquals(0, gbc.insets.top)
    }

    fun testModifyInsetsToMakeSpaceForCherry6() {
        listener.mainIndex = 5
        val gbc = GridBagConstraints()
        listener.modifyInsetsToMakeSpaceForCherry(gbc, 1)
        assertEquals(0, gbc.insets.top)
    }

    fun testMakeSpaceForCherryOnMainBranch000() {
        listener.mainIndex = 0
        listener.wasHoveringOnMainBranch = true
        listener.animationInProgress = true
        listener.makeSpaceForCherryOnMainBranch()
        verify(listener, never()).getConstraintsForRepositioning()
        assertTrue(listener.wasHoveringOnMainBranch)
    }

    fun testMakeSpaceForCherryOnMainBranch100() {
        listener.mainIndex = 1
        listener.wasHoveringOnMainBranch = true
        listener.animationInProgress = true
        listener.makeSpaceForCherryOnMainBranch()
        verify(listener, never()).getConstraintsForRepositioning()
        assertTrue(listener.wasHoveringOnMainBranch)
    }

    fun testMakeSpaceForCherryOnMainBranch010() {
        listener.mainIndex = 0
        listener.wasHoveringOnMainBranch = false
        listener.animationInProgress = true
        listener.makeSpaceForCherryOnMainBranch()
        verify(listener, never()).getConstraintsForRepositioning()
        assertTrue(listener.wasHoveringOnMainBranch)
    }

    fun testMakeSpaceForCherryOnMainBranch001() {
        listener.mainIndex = 0
        listener.wasHoveringOnMainBranch = true
        listener.animationInProgress = false
        listener.makeSpaceForCherryOnMainBranch()
        verify(listener, never()).getConstraintsForRepositioning()
        assertTrue(listener.wasHoveringOnMainBranch)
    }

    fun testMakeSpaceForCherryOnMainBranch101() {
        val event =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(20)
                `when`(yOnScreen).thenReturn(30)
            }

        listener.mousePressed(event)
        listener.mainIndex = 1
        listener.wasHoveringOnMainBranch = true
        listener.animationInProgress = false
        listener.makeSpaceForCherryOnMainBranch()
        verify(listener).getConstraintsForRepositioning()
        assertTrue(listener.wasHoveringOnMainBranch)
    }

    fun testMakeSpaceForCherryOnMainBranch011() {
        val event =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(20)
                `when`(yOnScreen).thenReturn(30)
            }

        listener.mousePressed(event)
        listener.mainIndex = 0
        listener.wasHoveringOnMainBranch = false
        listener.animationInProgress = false
        listener.makeSpaceForCherryOnMainBranch()
        verify(listener).getConstraintsForRepositioning()
        assertTrue(listener.wasHoveringOnMainBranch)
    }

    fun testDispose() {
        listener.dispose()
    }

    fun testFindCherryIndex() {
        val event =
            mock(MouseEvent::class.java).apply {
                `when`(xOnScreen).thenReturn(20)
                `when`(yOnScreen).thenReturn(30)
            }

        `when`(cherry.y).thenReturn(-1)
        listener =
            spy(
                CherryDragAndDropListener(
                    project,
                    cherry,
                    addedBranchPanel,
                    mainPanel,
                ),
            )
        listener.mousePressed(event)
        assertEquals(0, listener.findCherryIndex())

        `when`(cherry.y).thenReturn(1000)
        listener =
            spy(
                CherryDragAndDropListener(
                    project,
                    cherry,
                    addedBranchPanel,
                    mainPanel,
                ),
            )
        listener.mousePressed(event)
        assertEquals(3, listener.findCherryIndex())

        `when`(cherry.y).thenReturn(20)
        listener =
            spy(
                CherryDragAndDropListener(
                    project,
                    cherry,
                    addedBranchPanel,
                    mainPanel,
                ),
            )
        listener.mousePressed(event)
        assertEquals(1, listener.findCherryIndex())
    }
}
