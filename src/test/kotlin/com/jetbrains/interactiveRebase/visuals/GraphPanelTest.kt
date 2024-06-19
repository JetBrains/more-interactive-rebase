package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.GraphInfo
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.ActionService
import junit.framework.TestCase
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.awt.BasicStroke
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagLayout
import java.awt.LinearGradientPaint
import java.awt.RenderingHints
import java.awt.geom.CubicCurve2D

class GraphPanelTest : BasePlatformTestCase() {
    private lateinit var commitProvider: TestGitCommitProvider
    private lateinit var commit1: CommitInfo
    private lateinit var commit2: CommitInfo
    private lateinit var commit3: CommitInfo
    private lateinit var commit4: CommitInfo
    private lateinit var commit5: CommitInfo
    private lateinit var commit6: CommitInfo
    private lateinit var branchInfo: BranchInfo
    private lateinit var otherBranchInfo: BranchInfo
    private lateinit var graphInfo: GraphInfo
    private lateinit var mainCirclePanel: CirclePanel
    private lateinit var addedCirclePanel: CirclePanel
    private lateinit var graphPanel: GraphPanel

    override fun setUp() {
        super.setUp()
        commitProvider = TestGitCommitProvider(project)

        commit1 = CommitInfo(commitProvider.createCommit("One"), project, mutableListOf())
        commit2 = CommitInfo(commitProvider.createCommit("Two"), project, mutableListOf())
        commit3 = CommitInfo(commitProvider.createCommit("Three"), project, mutableListOf())
        branchInfo = BranchInfo("branch", mutableListOf(commit1, commit2, commit3))
        branchInfo.currentCommits = mutableListOf(commit1, commit2, commit3)

        mainCirclePanel = mock(CirclePanel::class.java)
        `when`(mainCirclePanel.x).thenReturn(10)
        `when`(mainCirclePanel.y).thenReturn(20)
        `when`(mainCirclePanel.width).thenReturn(30)
        `when`(mainCirclePanel.height).thenReturn(40)

        commit4 = CommitInfo(commitProvider.createCommit("Four"), project, mutableListOf())
        commit5 = CommitInfo(commitProvider.createCommit("Five"), project, mutableListOf())
        commit6 = CommitInfo(commitProvider.createCommit("Six"), project, mutableListOf())
        otherBranchInfo = BranchInfo("other", mutableListOf(commit4, commit5, commit6))
        otherBranchInfo.currentCommits = mutableListOf(commit4, commit5, commit6)
        otherBranchInfo.baseCommit = commit6

        addedCirclePanel = mock(CirclePanel::class.java)
        `when`(addedCirclePanel.x).thenReturn(15)
        `when`(addedCirclePanel.y).thenReturn(25)
        `when`(addedCirclePanel.width).thenReturn(35)
        `when`(addedCirclePanel.height).thenReturn(45)
        `when`(addedCirclePanel.commit).thenReturn(commit6)

        graphInfo = GraphInfo(branchInfo, otherBranchInfo)

        val mainPanel = mock(MainPanel::class.java)
        project.service<ActionService>().mainPanel = mainPanel
        `when`(mainPanel.getComponent(0)).thenReturn(DragPanel())

        graphPanel = spy(GraphPanel(project, graphInfo))
        graphPanel.mainBranchPanel.branchPanel.circles = mutableListOf(mainCirclePanel)
        graphPanel.addedBranchPanel!!.branchPanel.circles = mutableListOf(addedCirclePanel)
    }

    fun testGraphPanel() {
        assertThat(graphPanel.layout).isInstanceOf(GridBagLayout::class.java)
    }

    fun testCreateGraphPanelWithoutAddedBranch() {
        val graph = GraphInfo(graphInfo.mainBranch)
        val panel = GraphPanel(project, graph)
        assertNotNull(panel)
        assertNotNull(panel.mainBranchPanel)
        assertNull(panel.addedBranchPanel)
    }

    fun testCreateGraphPanelWithAddedBranch() {
        assertNotNull(graphPanel)
        assertNotNull(graphPanel.mainBranchPanel)
        assertNotNull(graphPanel.addedBranchPanel)
    }

    fun testCenterCoordinatesOfLastMainCircle() {
        val coordinates = graphPanel.centerCoordinatesOfLastMainCircle()
        assertEquals(25, coordinates.first)
        assertEquals(40, coordinates.second)
    }

    fun testCenterCoordinatesOfLastMainCircleEmptyList() {
        branchInfo.currentCommits = mutableListOf()
        branchInfo.isPrimary = true
        graphInfo = GraphInfo(branchInfo, otherBranchInfo)
        graphPanel = GraphPanel(project, graphInfo)
        val coordinates = graphPanel.centerCoordinatesOfLastMainCircle()
        assertEquals(0, coordinates.first)
        assertEquals(0, coordinates.second)
    }

    fun testCenterCoordinatesOfLastAddedCircle() {
        val g: Graphics = mock(Graphics::class.java)
        val g2d = mock(Graphics2D::class.java)
        `when`(g2d.create()).thenReturn(g)

        graphPanel.paintComponent(g2d)
        val coordinates = graphPanel.centerCoordinatesOfBaseCircleInAddedBranch()
        assertEquals(32, coordinates.first)
        assertEquals(47, coordinates.second)
    }

    fun testPaintComponent() {
        val g: Graphics = mock(Graphics::class.java)
        val g2d = mock(Graphics2D::class.java)
        `when`(g2d.create()).thenReturn(g)

        graphPanel.paintComponent(g2d)

        verify(g2d, times(1)).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(g2d, times(1)).color = Palette.BLUE
        verify(g2d, times(1)).stroke = BasicStroke(2f)
        verify(g2d, times(1)).draw(any(CubicCurve2D.Float::class.java))
    }

    fun testPaintComponentEmpty() {
        val g: Graphics = mock(Graphics::class.java)
        val g2d = mock(Graphics2D::class.java)
        `when`(g2d.create()).thenReturn(g)

        graphPanel.mainBranchPanel.branchPanel.circles = mutableListOf()
        graphPanel.paintComponent(g2d)

        verify(g2d, times(1)).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(g2d, times(1)).color = Palette.BLUE
        verify(g2d, times(1)).stroke = BasicStroke(2f)
        verify(g2d, never()).draw(any(CubicCurve2D.Float::class.java))
    }

    fun testPaintComponentOneBranchOnly() {
        val g: Graphics = mock(Graphics::class.java)
        val g2d = mock(Graphics2D::class.java)
        `when`(g2d.create()).thenReturn(g)

        graphPanel.addedBranchPanel = null
        graphPanel.paintComponent(g2d)

        verify(g2d, times(1)).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(g2d, times(1)).color = Palette.BLUE
        verify(g2d, times(1)).stroke = BasicStroke(2f)
        verify(g2d, never()).draw(any(CubicCurve2D.Float::class.java))
    }

    fun testPaintComponentZero() {
        addedCirclePanel = mock(CirclePanel::class.java)
        `when`(addedCirclePanel.x).thenReturn(0)
        `when`(addedCirclePanel.y).thenReturn(25)
        `when`(addedCirclePanel.width).thenReturn(0)
        `when`(addedCirclePanel.height).thenReturn(45)
        `when`(addedCirclePanel.commit).thenReturn(commit6)

        val g: Graphics = mock(Graphics::class.java)
        val g2d = mock(Graphics2D::class.java)
        `when`(g2d.create()).thenReturn(g)

        graphPanel = spy(GraphPanel(project, graphInfo))
        graphPanel.mainBranchPanel.branchPanel.circles = mutableListOf(mainCirclePanel)
        graphPanel.addedBranchPanel!!.branchPanel.circles = mutableListOf(addedCirclePanel)

        graphPanel.paintComponent(g2d)

        verify(g2d, times(1)).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(g2d, times(1)).color = Palette.BLUE
        verify(g2d, times(1)).stroke = BasicStroke(2f)
        verify(g2d, never()).draw(any(CubicCurve2D.Float::class.java))

        addedCirclePanel = mock(CirclePanel::class.java)
        `when`(addedCirclePanel.x).thenReturn(25)
        `when`(addedCirclePanel.y).thenReturn(0)
        `when`(addedCirclePanel.width).thenReturn(35)
        `when`(addedCirclePanel.height).thenReturn(0)
        `when`(addedCirclePanel.commit).thenReturn(commit6)

        graphPanel = spy(GraphPanel(project, graphInfo))
        graphPanel.mainBranchPanel.branchPanel.circles = mutableListOf(mainCirclePanel)
        graphPanel.addedBranchPanel!!.branchPanel.circles = mutableListOf(addedCirclePanel)

        graphPanel.paintComponent(g2d)

        verify(g2d, times(2)).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(g2d, times(2)).color = Palette.BLUE
        verify(g2d, times(2)).stroke = BasicStroke(2f)
        verify(g2d, never()).draw(any(CubicCurve2D.Float::class.java))
    }

    fun testPaintComponentNoLine() {
        addedCirclePanel = mock(CirclePanel::class.java)
        `when`(addedCirclePanel.x).thenReturn(0)
        `when`(addedCirclePanel.y).thenReturn(0)
        `when`(addedCirclePanel.width).thenReturn(0)
        `when`(addedCirclePanel.height).thenReturn(0)
        `when`(addedCirclePanel.commit).thenReturn(commit6)

        graphPanel = spy(GraphPanel(project, graphInfo))
        graphPanel.mainBranchPanel.branchPanel.circles = mutableListOf(mainCirclePanel)
        graphPanel.addedBranchPanel!!.branchPanel.circles = mutableListOf(addedCirclePanel)

        val g: Graphics = mock(Graphics::class.java)
        val g2d = mock(Graphics2D::class.java)
        `when`(g2d.create()).thenReturn(g)
        graphPanel.paintComponent(g2d)

        verify(g2d, times(1)).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(g2d, times(1)).color = Palette.BLUE
        verify(g2d, times(1)).stroke = BasicStroke(2f)
        verify(g2d, never()).draw(any(CubicCurve2D.Float::class.java))
    }

    fun testPaintComponentNoBase() {
        addedCirclePanel = mock(CirclePanel::class.java)
        `when`(addedCirclePanel.x).thenReturn(0)
        `when`(addedCirclePanel.y).thenReturn(0)
        `when`(addedCirclePanel.width).thenReturn(0)
        `when`(addedCirclePanel.height).thenReturn(0)

        graphPanel = spy(GraphPanel(project, graphInfo))
        graphPanel.mainBranchPanel.branchPanel.circles = mutableListOf(mainCirclePanel)
        graphPanel.addedBranchPanel!!.branchPanel.circles = mutableListOf(addedCirclePanel)

        val g: Graphics = mock(Graphics::class.java)
        val g2d = mock(Graphics2D::class.java)
        `when`(g2d.create()).thenReturn(g)
        assertThatThrownBy {
            graphPanel.paintComponent(g2d)
        }
            .isInstanceOf(UninitializedPropertyAccessException::class.java)
    }

    fun testGradientTransition() {
        val g2d = mock(Graphics2D::class.java)

        graphPanel.gradientTransition(g2d, 0, 0, 100, 100)

        verify(g2d, times(1)).setPaint(
            any(LinearGradientPaint::class.java),
        )
    }

    fun testUpdateGraphPanel() {
        graphPanel.updateGraphPanel()
        verify(graphPanel).removeAll()
        verify(graphPanel).addBranches()
        verify(graphPanel).repaint()
    }

    fun testUpdateGraphPanelSingleBranch() {
        graphPanel = spy(GraphPanel(project, GraphInfo(branchInfo)))
        graphPanel.updateGraphPanel()
        verify(graphPanel).removeAll()
        verify(graphPanel).addBranches()
        verify(graphPanel).repaint()
        TestCase.assertNull(graphPanel.addedBranchPanel)
    }

    fun testComputeVerticalOffsets() {
        val result = graphPanel.computeVerticalOffsets()
        assertThat(result).isEqualTo(Pair(5, 5))

        otherBranchInfo.currentCommits = mutableListOf(commit4, commit5, commit6)
        otherBranchInfo.baseCommit = commit4

        graphInfo = GraphInfo(branchInfo, otherBranchInfo)
        graphPanel = spy(GraphPanel(project, graphInfo))

        graphPanel.mainBranchPanel.branchPanel.circles =
            mutableListOf(mainCirclePanel, mainCirclePanel, mainCirclePanel, mainCirclePanel)

        val result2 = graphPanel.computeVerticalOffsets()
        assertThat(result2).isEqualTo(Pair(5, 300))

        otherBranchInfo.currentCommits = mutableListOf(commit1, commit2, commit4, commit5, commit6)
        otherBranchInfo.baseCommit = commit6

        graphInfo = GraphInfo(branchInfo, otherBranchInfo)
        graphPanel = spy(GraphPanel(project, graphInfo))

        graphPanel.mainBranchPanel.branchPanel.circles = mutableListOf(mainCirclePanel)

        val result3 = graphPanel.computeVerticalOffsets()
        assertThat(result3).isEqualTo(Pair(120, 5))

        otherBranchInfo.currentCommits = mutableListOf(commit1, commit2, commit4, commit5, commit6)
        otherBranchInfo.baseCommit = commit3

        graphInfo = GraphInfo(branchInfo, otherBranchInfo)
        graphPanel = spy(GraphPanel(project, graphInfo))

        graphPanel.mainBranchPanel.branchPanel.circles = mutableListOf(mainCirclePanel)

        val result4 = graphPanel.computeVerticalOffsets()
        assertThat(result4).isEqualTo(Pair(5, 180))
    }
}
