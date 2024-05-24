package com.jetbrains.interactiveRebase.visuals

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import org.assertj.core.api.Assertions.assertThat
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

class StopToEditCirclePanelTest : BasePlatformTestCase() {
    private lateinit var stopToEditCirclePanel: StopToEditCirclePanel
    private lateinit var commit: CommitInfo
    private lateinit var g: Graphics2D

    @Captor
    private lateinit var colorCaptor: ArgumentCaptor<Color>

    override fun setUp() {
        super.setUp()
        val commitProvider = TestGitCommitProvider(project)
        commit = CommitInfo(commitProvider.createCommit("LOL"), project, mutableListOf())
        g = mock(Graphics2D::class.java)

        stopToEditCirclePanel =
            spy(
                StopToEditCirclePanel(
                    50.0,
                    2.0f,
                    JBColor.BLACK,
                    commit,
                ),
            )
        openMocks(this)

        doAnswer {
            commit
        }.`when`(stopToEditCirclePanel).commit
    }

    fun testPaintCircleSelected() {
        commit.isSelected = true
        stopToEditCirclePanel.paintCircle(g)
        verify(g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(g, times(2)).fill(any(Ellipse2D.Double::class.java))
        verify(g).stroke = any(BasicStroke::class.java)
        verify(g, times(1)).draw(any(Ellipse2D.Double::class.java))

        verify(g, times(2)).color = colorCaptor.capture()

        assertEquals(2, colorCaptor.allValues.size)
        colorEquals(colorCaptor.allValues[0], Palette.DARKGRAY.darker().darker())
        colorEquals(colorCaptor.allValues[1], Palette.BLUEBORDER.darker())
    }

    fun testPaintCircleHovered() {
        commit.isHovered = true
        stopToEditCirclePanel.paintCircle(g)

        verify(g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(g, times(2)).fill(any(Ellipse2D.Double::class.java))
        verify(g, times(2)).stroke = any(BasicStroke::class.java)
        verify(g, times(2)).draw(any(Ellipse2D.Double::class.java))
        verify(g, times(2)).stroke = any(BasicStroke::class.java)
        verify(g, times(3)).color = colorCaptor.capture()

        assertEquals(3, colorCaptor.allValues.size)
        colorEquals(Palette.JETBRAINSGRAY, colorCaptor.allValues[0])
        colorEquals(Palette.DARKBLUE, colorCaptor.allValues[1])
        colorEquals(JBColor.BLACK, colorCaptor.allValues[2])
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
