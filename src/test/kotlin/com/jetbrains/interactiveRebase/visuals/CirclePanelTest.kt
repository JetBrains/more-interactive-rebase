package com.jetbrains.interactiveRebase.visuals

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import git4idea.GitCommit
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.plaf.ComponentUI

class CirclePanelTest : BasePlatformTestCase() {
    private lateinit var graph: Graphics2D
    private lateinit var graph2: Graphics
    private lateinit var ui: ComponentUI
    private lateinit var commit: CommitInfo

    override fun setUp() {
        super.setUp()
        graph = mock(Graphics2D::class.java)
        graph2 = mock(Graphics::class.java)
        ui = mock(ComponentUI::class.java)
        commit = CommitInfo(mock(GitCommit::class.java), project, mutableListOf())
    }

    fun testPaintComponent() {
        val circlePanel = CirclePanel(10.0, 2f, JBColor.BLUE, commit)
        `when`(graph.create()).thenReturn(graph2)

        circlePanel.paintComponent(graph)

        verify(graph2).dispose()
        verify(graph, times(1)).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(graph, times(3)).fill(circlePanel.circle)
    }

    fun testPaintComponentIsSelected() {
        val circlePanel = CirclePanel(10.0, 2f, JBColor.BLUE, commit)
        `when`(graph.create()).thenReturn(graph2)
        circlePanel.commit.isSelected = true
        circlePanel.commit.isHovered = true
        circlePanel.paintComponent(graph)

        verify(graph2).dispose()
        verify(graph, times(1)).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(graph, times(3)).draw(circlePanel.circle)
        verify(graph, times(3)).fill(circlePanel.circle)
    }
}
