package com.jetbrains.interactiveRebase.visuals

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import git4idea.GitCommit
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Captor
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
        commit = CommitInfo(mock(GitCommit::class.java), project, mutableListOf())
        g = mock(Graphics2D::class.java)

        dropCirclePanel =
            spy(
                DropCirclePanel(
                    diameter = 50.0,
                    border = 2.0f,
                    color = JBColor.BLACK,
                    commit = commit,
                ),
            )
        openMocks(this)
    }

    fun testPaintCircleSelected() {
        commit.isSelected = true
        dropCirclePanel.paintCircle(g)
        verify(g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(g, times(7)).fill(any(Ellipse2D.Double::class.java))
        verify(g).stroke = any(BasicStroke::class.java)
        verify(g, times(2)).draw(any(Ellipse2D.Double::class.java))

        verify(g, times(7)).color = colorCaptor.capture()

        assertEquals(7, colorCaptor.allValues.size)
        colorEquals(Palette.SELECTEDHIGHLIGHT, colorCaptor.allValues[0])
        colorEquals(Palette.SELECTEDHIGHLIGHT, colorCaptor.allValues[1])
        colorEquals(Palette.SELECTEDHIGHLIGHT, colorCaptor.allValues[2])
        colorEquals(Palette.SELECTEDHIGHLIGHT, colorCaptor.allValues[3])
        colorEquals(Palette.SELECTEDHIGHLIGHT, colorCaptor.allValues[4])
    }

    fun testPaintCircleHovered() {
        dropCirclePanel.paintCircle(g)

        verify(g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(g, times(7)).fill(any(Ellipse2D.Double::class.java))
        verify(g).stroke = any(BasicStroke::class.java)
        verify(g, times(2)).draw(any(Ellipse2D.Double::class.java))
        verify(g).stroke = any(BasicStroke::class.java)
        verify(g, times(7)).color = colorCaptor.capture()

        assertEquals(7, colorCaptor.allValues.size)
        colorEquals(Palette.DARKSHADOW, colorCaptor.allValues[0])
        colorEquals(Palette.DARKSHADOW, colorCaptor.allValues[1])
        colorEquals(Palette.DARKSHADOW, colorCaptor.allValues[2])
        colorEquals(Palette.DARKSHADOW, colorCaptor.allValues[3])
        colorEquals(Palette.DARKSHADOW, colorCaptor.allValues[4])
        colorEquals(Palette.GRAY, colorCaptor.allValues[5])
        colorEquals(Palette.BLUEBORDER, colorCaptor.allValues[6])
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

    private inline fun <reified T> anyCustom(): T = any(T::class.java)

    private fun colorEquals(
        expected: Color,
        actual: Color,
    ) {
        assertEquals(expected.red, actual.red)
        assertEquals(expected.green, actual.green)
        assertEquals(expected.blue, actual.blue)
    }
}
