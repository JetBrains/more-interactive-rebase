package com.jetbrains.interactiveRebase.visuals

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Captor
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
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
        commit.isSelected = true
        squashedCirclePanel.paintCircle(g)
        verify(g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(g, times(4)).fill(any(Ellipse2D.Double::class.java))
        verify(g, times(2)).stroke = any(BasicStroke::class.java)
        verify(g, times(2)).draw(any(Ellipse2D.Double::class.java))

        verify(g, times(4)).color = colorCaptor.capture()

        assertEquals(4, colorCaptor.allValues.size)
    }

    fun testPaintCircleHovered() {
        commit.isHovered = true
        squashedCirclePanel.paintCircle(g)

        verify(g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(g, times(4)).fill(any(Ellipse2D.Double::class.java))
        verify(g, times(3)).stroke = any(BasicStroke::class.java)
        verify(g, times(3)).draw(any(Ellipse2D.Double::class.java))
        verify(g, times(3)).stroke = any(BasicStroke::class.java)
        verify(g, times(5)).color = colorCaptor.capture()

        assertEquals(5, colorCaptor.allValues.size)
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
}
