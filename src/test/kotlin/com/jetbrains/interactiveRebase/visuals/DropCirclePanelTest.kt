package com.jetbrains.interactiveRebase.visuals

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertArrayEquals
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

class DropCirclePanelTest : BasePlatformTestCase() {
    private lateinit var dropCirclePanel: DropCirclePanel
    private lateinit var commit: CommitInfo
    private lateinit var g: Graphics2D

    @Captor
    private lateinit var colorCaptor: ArgumentCaptor<Color>

    override fun setUp() {
        super.setUp()
        val commitProvider = TestGitCommitProvider(project)
        commit = CommitInfo(commitProvider.createCommit("tests"), project, mutableListOf())
        g = mock(Graphics2D::class.java)

        dropCirclePanel =
            spy(
                DropCirclePanel(
                    50.0,
                    2.0f,
                    Palette.BLUE_THEME,
                    commit,
                ),
            )
        openMocks(this)

        doAnswer {
            commit
        }.`when`(dropCirclePanel).commit
    }

    fun testPaintCircleSelected() {
        commit.isSelected = true
        dropCirclePanel.paintCircle(g)
        verify(g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(g, times(2)).fill(any(Ellipse2D.Double::class.java))
        verify(g).stroke = any(BasicStroke::class.java)
        verify(g, times(1)).draw(any(Ellipse2D.Double::class.java))

        verify(g, times(2)).color = colorCaptor.capture()

        assertEquals(2, colorCaptor.allValues.size)
        colorEquals(colorCaptor.allValues[0], Palette.GRAY.darker().darker())
        colorEquals(colorCaptor.allValues[1], Palette.DARK_BLUE.darker())
    }

    fun testPaintCircleHovered() {
        commit.isHovered = true
        dropCirclePanel.paintCircle(g)

        verify(g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(g, times(2)).fill(any(Ellipse2D.Double::class.java))
        verify(g, times(2)).stroke = any(BasicStroke::class.java)
        verify(g, times(2)).draw(any(Ellipse2D.Double::class.java))
        verify(g, times(2)).stroke = any(BasicStroke::class.java)
        verify(g, times(3)).color = colorCaptor.capture()

        assertEquals(3, colorCaptor.allValues.size)
        colorEquals(Palette.GRAY, colorCaptor.allValues[0])
        colorEquals(Palette.DARK_BLUE, colorCaptor.allValues[1])
        colorEquals(Palette.DARK_BLUE, colorCaptor.allValues[1])
        colorEquals(JBColor.BLACK, colorCaptor.allValues[2])
    }

    fun testDrawBorder() {
        val circle = Ellipse2D.Double(0.0, 0.0, 50.0, 50.0)
        val borderColor = JBColor.BLUE

        dropCirclePanel.drawBorder(g, circle, borderColor)

        verify(g).fill(circle)
        verify(g).color = borderColor

        val captor = ArgumentCaptor.forClass(BasicStroke::class.java)
        verify(g).stroke = captor.capture()
        assertEquals(1.5f * 2.0f, captor.value.lineWidth)
        assertEquals(BasicStroke.CAP_BUTT, captor.value.endCap)
        assertEquals(BasicStroke.JOIN_MITER, captor.value.lineJoin)
        assertEquals(10f, captor.value.miterLimit)
        assertArrayEquals(floatArrayOf(3f, 3f), captor.value.dashArray)
        assertEquals(0f, captor.value.dashPhase)
        verify(g).draw(circle)
    }

    private fun colorEquals(
        expected: Color,
        actual: Color,
    ) {
        assertThat(expected.red).isEqualTo(actual.red)
        assertThat(expected.green).isEqualTo(actual.green)
        assertThat(expected.blue).isEqualTo(actual.blue)
    }
}
