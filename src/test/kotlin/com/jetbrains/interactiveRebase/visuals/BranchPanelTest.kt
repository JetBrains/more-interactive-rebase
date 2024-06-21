package com.jetbrains.interactiveRebase.visuals

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.StopToEditCommand
import git4idea.GitCommit
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.plaf.ComponentUI

class BranchPanelTest : BasePlatformTestCase() {
    private lateinit var graph: Graphics2D
    private lateinit var graph2: Graphics
    private lateinit var commit1: CommitInfo
    private lateinit var commit2: CommitInfo
    private lateinit var commit3: CommitInfo
    private lateinit var ui: ComponentUI
    private lateinit var branchPanel: BranchPanel

    override fun setUp() {
        super.setUp()
        graph = mock(Graphics2D::class.java)
        graph2 = mock(Graphics::class.java)
        ui = mock(ComponentUI::class.java)
        commit1 = CommitInfo(mock(GitCommit::class.java), project, mutableListOf())
        commit2 = CommitInfo(mock(GitCommit::class.java), project, mutableListOf())
        commit3 = CommitInfo(mock(GitCommit::class.java), project, mutableListOf())
        val branch = BranchInfo("branch", mutableListOf(commit1, commit2, commit3))
        branch.currentCommits = mutableListOf(commit1, commit2, commit3)
        branchPanel = spy(BranchPanel(branch, Palette.BLUE_THEME))

        commit2.changes.add(StopToEditCommand(commit2))
        commit3.changes.add(DropCommand(commit3))
    }

    fun testPaintComponent() {
        val circle1 = mock(CirclePanel::class.java)
        `when`(circle1.y).thenReturn(0)
        `when`(circle1.height).thenReturn(30)
        `when`(circle1.colorTheme).thenReturn(Palette.BLUE_THEME)
        val circle2 = mock(CirclePanel::class.java)
        `when`(circle2.y).thenReturn(0)
        `when`(circle2.height).thenReturn(30)
        `when`(circle2.colorTheme).thenReturn(Palette.BLUE_THEME)

        branchPanel.circles[0] = circle1
        branchPanel.circles[1] = circle2

        `when`(graph.create()).thenReturn(graph2)

        branchPanel.paintComponent(graph)

        verify(graph2).dispose()
        verify(graph, times(1)).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    }

    fun testPaintComponentEmpty() {
        val branch = BranchInfo("branch", mutableListOf())
        branch.currentCommits = mutableListOf()
        branch.isPrimary = true
        branchPanel = spy(BranchPanel(branch, Palette.BLUE_THEME))

        `when`(graph.create()).thenReturn(graph2)

        branchPanel.paintComponent(graph)

        verify(graph2).dispose()
        verify(graph, times(1)).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    }

    fun testPaintComponentEmptyNotPrimary() {
        val branch = BranchInfo("branch", mutableListOf())
        branch.currentCommits = mutableListOf()
        branch.isPrimary = false
        branchPanel = spy(BranchPanel(branch, Palette.BLUE_THEME))

        `when`(graph.create()).thenReturn(graph2)

        branchPanel.paintComponent(graph)

        verify(graph2).dispose()
        verify(graph, times(1)).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    }

    fun testPaintComponentNotEmptyNotPrimary() {
        val branch = BranchInfo("branch", mutableListOf(commit1))
        branch.currentCommits = mutableListOf(commit1)
        branch.isPrimary = false
        branchPanel = spy(BranchPanel(branch, Palette.BLUE_THEME))

        `when`(graph.create()).thenReturn(graph2)

        branchPanel.paintComponent(graph)

        verify(graph2).dispose()
        verify(graph, times(1)).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    }

    fun testColor() {
        assertEquals(branchPanel.colorTheme, Palette.BLUE_THEME)
    }

    fun testBranchSize() {
        assertEquals(branchPanel.borderSize, 1f)
    }

    fun testInitializeCirclePanelDrop() {
        val circle = branchPanel.initializeCirclePanel(2)
        assertTrue(circle is DropCirclePanel)
        assertEquals(circle.commit, commit3)
    }

    fun testInitializeCirclePanelPaused() {
        commit2.isPaused = true

        val circle = branchPanel.initializeCirclePanel(1)
        assertThat(circle.colorTheme).isEqualTo(Palette.LIME_THEME)
    }

    fun testInitializeCirclePanelRebased() {
        commit1.isRebased = true

        val circle = branchPanel.initializeCirclePanel(0)
        assertThat(circle.colorTheme).isEqualTo(Palette.LIME_GREEN_THEME)
    }

    fun testInitializeCirclePanelStopToEdit() {
        val circle = branchPanel.initializeCirclePanel(1)
        assertTrue(circle is StopToEditCirclePanel)
        assertEquals(circle.commit, commit2)
    }
}
