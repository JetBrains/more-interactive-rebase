package com.jetbrains.interactiveRebase.visuals

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import org.assertj.core.api.Assertions.assertThat
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
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

        addedCirclePanel = mock(CirclePanel::class.java)
        `when`(addedCirclePanel.x).thenReturn(15)
        `when`(addedCirclePanel.y).thenReturn(25)
        `when`(addedCirclePanel.width).thenReturn(35)
        `when`(addedCirclePanel.height).thenReturn(45)

        graphPanel = GraphPanel(project, branchInfo, otherBranchInfo)
        graphPanel.mainBranchPanel.branchPanel.circles = mutableListOf(mainCirclePanel)
        graphPanel.addedBranchPanel!!.branchPanel.circles = mutableListOf(addedCirclePanel)
    }

    fun testGraphPanel() {
        assertThat(graphPanel.layout).isInstanceOf(GridBagLayout::class.java)
    }

    fun testCreateGraphPanelWithoutAddedBranch() {
        val panel = GraphPanel(project, branchInfo)
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

    fun testCenterCoordinatesOfLastAddedCircle() {
        val coordinates = graphPanel.centerCoordinatesOfLastAddedCircle()
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

    fun testGradientTransition() {
        val g2d = mock(Graphics2D::class.java)

        graphPanel.gradientTransition(g2d, 0, 0, 100, 100)

        verify(g2d, times(1)).setPaint(
            any(LinearGradientPaint::class.java),
        )
    }
}
