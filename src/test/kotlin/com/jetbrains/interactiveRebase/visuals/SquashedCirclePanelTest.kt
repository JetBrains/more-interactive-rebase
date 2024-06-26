package com.jetbrains.interactiveRebase.visuals

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.StopToEditCommand
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Captor
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations.openMocks
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D

class SquashedCirclePanelTest : BasePlatformTestCase() {
    private lateinit var squashedCirclePanel: SquashedCirclePanel
    private lateinit var commit: CommitInfo
    private lateinit var g: Graphics2D

    @Captor
    private lateinit var colorCaptor: ArgumentCaptor<Color>

    override fun setUp() {
        super.setUp()
        val commitProvider = TestGitCommitProvider(project)
        commit = CommitInfo(commitProvider.createCommit("tests"), project, mutableListOf())
        g = mock(Graphics2D::class.java)

        squashedCirclePanel =
            spy(
                SquashedCirclePanel(
                    50.0,
                    2.0f,
                    Palette.BLUE_THEME,
                    commit,
                ),
            )
        openMocks(this)

        doAnswer {
            commit
        }.`when`(squashedCirclePanel).commit
    }

    fun testPaintCircleSelected() {
        squashedCirclePanel =
            spy(
                SquashedCirclePanel(
                    50.0,
                    2.0f,
                    Palette.BLUE_THEME,
                    commit,
                    mock(CirclePanel::class.java),
                ),
            )
        commit.isSelected = true
        squashedCirclePanel.paintCircle(g)
        verify(g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(g, times(5)).fill(any(Ellipse2D.Double::class.java))
        verify(g, times(3)).stroke = any(BasicStroke::class.java)
        verify(g, times(3)).draw(any(Ellipse2D.Double::class.java))

        verify(g, times(6)).color = colorCaptor.capture()

        assertEquals(6, colorCaptor.allValues.size)
    }

    fun testPaintCircleHovered() {
        squashedCirclePanel =
            spy(
                SquashedCirclePanel(
                    50.0,
                    2.0f,
                    Palette.BLUE_THEME,
                    commit,
                    previous = mock(CirclePanel::class.java),
                ),
            )
        val parent = mock(JBPanel<JBPanel<*>>()::class.java)
        `when`(squashedCirclePanel.parent).thenReturn(parent)
        `when`(parent.background).thenReturn(JBColor.BLACK)
        commit.isHovered = true
        squashedCirclePanel.paintCircle(g)

        verify(g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(g, times(7)).fill(any(Ellipse2D.Double::class.java))
        verify(g, times(4)).stroke = any(BasicStroke::class.java)
        verify(g, times(6)).draw(any(Ellipse2D.Double::class.java))
        verify(g, times(4)).stroke = any(BasicStroke::class.java)
        verify(g, times(11)).color = colorCaptor.capture()

        assertEquals(11, colorCaptor.allValues.size)
    }

    fun testPaintCircleStopToEdit() {
        val parent = mock(JBPanel<JBPanel<*>>()::class.java)
        `when`(squashedCirclePanel.parent).thenReturn(parent)
        val circle = mock(Ellipse2D.Double::class.java)
        `when`(squashedCirclePanel.circle).thenReturn(circle)
        `when`(parent.background).thenReturn(JBColor.BLACK)
        commit.isHovered = true
        commit.changes.add(StopToEditCommand(commit))
        squashedCirclePanel.paintCircle(g)

        verify(g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(g, times(7)).fill(any(Ellipse2D.Double::class.java))
        verify(g, times(4)).stroke = any(BasicStroke::class.java)
        verify(g, times(6)).draw(any(Ellipse2D.Double::class.java))
        verify(g, times(4)).stroke = any(BasicStroke::class.java)
        verify(g, times(11)).color = colorCaptor.capture()
        verify(squashedCirclePanel, times(1)).paintPauseInsideSquash(g, circle)

        assertEquals(11, colorCaptor.allValues.size)
    }

    fun testDrawBorder() {
        val circle = Ellipse2D.Double(0.0, 0.0, 50.0, 50.0)
        val borderColor = JBColor.BLUE

        squashedCirclePanel.drawBorder(g, circle, borderColor)

        verify(g).fill(circle)
        verify(g).color = borderColor

        val captor = ArgumentCaptor.forClass(BasicStroke::class.java)
        verify(g).stroke = captor.capture()
        assertEquals(2.0f, captor.value.lineWidth)
        verify(g).draw(circle)
    }

    fun testInterpolateColors() {
        val result = squashedCirclePanel.interpolateColors(Color.WHITE, Color.BLACK, 0.6f)
        assertEquals(Color(102, 102, 102), result)
    }
}
