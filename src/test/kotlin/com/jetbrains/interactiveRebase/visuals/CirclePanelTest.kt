package com.jetbrains.interactiveRebase.visuals

import com.intellij.icons.AllIcons
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import git4idea.GitCommit
import icons.DvcsImplIcons
import junit.framework.TestCase
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
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
        commit = CommitInfo(mock(GitCommit::class.java), project, mutableListOf(), false, false)
    }

    fun testPaintComponent() {
        val circlePanel = CirclePanel(10.0, 2f, Palette.BLUE_THEME, commit)
        `when`(graph.create()).thenReturn(graph2)

        circlePanel.paintComponent(graph)

        verify(graph2).dispose()
        verify(graph, times(1)).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(graph, times(2)).fill(circlePanel.circle)
    }

    fun testPaintComponentIsSelected() {
        val circlePanel = CirclePanel(10.0, 2f, Palette.BLUE_THEME, commit)
        `when`(graph.create()).thenReturn(graph2)
        circlePanel.commit.isSelected = true
        circlePanel.paintComponent(graph)

        verify(graph2).dispose()
        verify(graph, times(1)).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(graph, times(1)).draw(circlePanel.circle)
        verify(graph, times(2)).fill(circlePanel.circle)
    }

    fun testPaintCircleIsHovered() {
        val circlePanel =
            spy(
                CirclePanel(
                    10.0,
                    2f,
                    Palette.BLUE_THEME,
                    commit,
                    mock(CirclePanel::class.java),
                ),
            )
        `when`(graph.create()).thenReturn(graph2)
        circlePanel.commit.isHovered = true
        circlePanel.commit.wasCherryPicked = true
        circlePanel.commit.isPaused = true
        circlePanel.paintCircle(graph)

        verify(graph, times(1)).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(graph, times(2)).draw(circlePanel.circle)
        verify(graph, times(2)).fill(circlePanel.circle)
        verify(circlePanel, times(1)).icon(graph, DvcsImplIcons.CherryPick)
        verify(circlePanel, times(1)).icon(graph, AllIcons.General.InspectionsWarningEmpty)
    }

    fun testDrawBorder() {
        val circlePanel =
            CirclePanel(
                10.0,
                2f,
                Palette.BLUE_THEME,
                commit,
                previous = mock(CirclePanel::class.java),
            )
        val circle = circlePanel.circle
        val borderColor = JBColor.BLACK
        circlePanel.drawBorder(graph, circle, borderColor)
        verify(graph).fill(circle)
        verify(graph).color = borderColor
        verify(graph).draw(circle)
    }

    fun testSelectedCommitAppearance() {
        val circlePanel = CirclePanel(10.0, 2f, Palette.BLUE_THEME, commit)
        val circleColor = JBColor.BLACK
        val borderColor = JBColor.BLACK
        circlePanel.selectedCommitAppearance(graph, true, circleColor, borderColor)
        verify(graph, times(2)).fill(circlePanel.circle)
    }

    fun testColorCircle() {
        commit.isDragged = true
        commit.isSelected = true

        val circle = CirclePanel(10.0, 2f, Palette.BLUE_THEME, commit)

        TestCase.assertEquals(
            circle.colorCircle(),
            Pair(
                Palette.BLUE_THEME.draggedCircleColor.darker(),
                Palette.BLUE_THEME.selectedBorderColor,
            ),
        )
    }

    fun testColorCircleNotSelected() {
        commit.isDragged = true
        commit.isSelected = false

        val circle = CirclePanel(10.0, 2f, Palette.BLUE_THEME, commit)

        TestCase.assertEquals(
            circle.colorCircle(),
            Pair(
                Palette.BLUE_THEME.draggedCircleColor,
                Palette.BLUE_THEME.reorderedBorderColor,
            ),
        )
    }

    fun testColorCircleNotDragged() {
        commit.isDragged = false
        commit.isSelected = true

        val circle = CirclePanel(10.0, 2f, Palette.BLUE_THEME, commit)

        TestCase.assertEquals(
            circle.colorCircle(),
            Pair(
                Palette.BLUE_THEME.regularCircleColor.darker(),
                Palette.BLUE_THEME.selectedBorderColor,
            ),
        )
    }

    fun testColorCircleNotDraggedNotSelected() {
        commit.isDragged = false
        commit.isSelected = false

        val circle = CirclePanel(10.0, 2f, Palette.BLUE_THEME, commit)

        TestCase.assertEquals(
            circle.colorCircle(),
            Pair(
                Palette.BLUE_THEME.regularCircleColor,
                Palette.BLUE_THEME.borderColor,
            ),
        )
    }

    fun testColorCircleReordered() {
        commit.isDragged = false
        commit.isSelected = false
        commit.isReordered = true

        val circle = CirclePanel(10.0, 2f, Palette.BLUE_THEME, commit)

        TestCase.assertEquals(
            circle.colorCircle(),
            Pair(
                Palette.BLUE_THEME.regularCircleColor,
                Palette.BLUE_THEME.reorderedBorderColor,
            ),
        )
    }
//        var circleColor: Color =
//            if (commit.isDragged) {
//                colorTheme.draggedCircleColor
//            } else {
//                colorTheme.regularCircleColor
//            }
//        circleColor = if (commit.isSelected) circleColor.darker() else circleColor
//        val borderColor =
//            if (commit.isSelected) {
//                colorTheme.selectedBorderColor
//            } else if (commit.isDragged || commit.isReordered) {
//                colorTheme.reorderedBorderColor
//            } else {
//                colorTheme.borderColor
//            }
//        return Pair(circleColor, borderColor)
//    }
}
