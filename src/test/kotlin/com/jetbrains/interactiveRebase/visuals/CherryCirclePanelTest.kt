package com.jetbrains.interactiveRebase.visuals

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import icons.DvcsImplIcons
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.awt.BasicStroke
import java.awt.Graphics
import java.awt.Graphics2D

class CherryCirclePanelTest : BasePlatformTestCase() {
    private lateinit var cherry: CherryCirclePanel
    private lateinit var commit: CommitInfo
    private lateinit var g2d: Graphics2D

    override fun setUp() {
        super.setUp()
        val g: Graphics = mock(Graphics::class.java)
        g2d = mock(Graphics2D::class.java)
        `when`(g2d.create()).thenReturn(g)
    }

    fun testPaintCircle() {
        commit = mock(CommitInfo::class.java)
        `when`(commit.isSelected).thenReturn(false)
        `when`(commit.isHovered).thenReturn(false)
        cherry =
            spy(
                CherryCirclePanel(
                    30.0,
                    2f,
                    Palette.BLUE_THEME,
                    commit,
                    next = mock(CirclePanel::class.java),
                    isModifiable = true,
                ),
            )
        cherry.paintCircle(g2d)

        assertThat(cherry.colorTheme)
            .isEqualTo(Palette.BLUE_THEME)

        val circleColor = Palette.BLUE_THEME.regularCircleColor
        val borderColor = Palette.BLUE_THEME.borderColor
        verify(cherry).selectedCommitAppearance(g2d, false, circleColor, borderColor)

        verify(g2d, never()).color = JBColor.BLACK
        verify(g2d, times(1)).stroke = BasicStroke(2f)
        verify(g2d, times(1)).draw(cherry.circle)
        verify(cherry).icon(g2d, DvcsImplIcons.CherryPick)
    }

    fun testPaintCircleSelected() {
        commit = mock(CommitInfo::class.java)
        `when`(commit.isSelected).thenReturn(true)
        `when`(commit.isHovered).thenReturn(false)
        cherry =
            spy(
                CherryCirclePanel(
                    30.0,
                    2f,
                    Palette.BLUE_THEME,
                    commit,
                    previous = mock(CirclePanel::class.java),
                ),
            )
        cherry.paintCircle(g2d)

        assertThat(cherry.colorTheme)
            .isEqualTo(Palette.BLUE_THEME)

        val circleColor = Palette.BLUE_THEME.regularCircleColor.darker()
        val borderColor = Palette.BLUE_THEME.borderColor.darker()
        verify(cherry).selectedCommitAppearance(g2d, true, circleColor, borderColor)

        verify(g2d, never()).color = JBColor.BLACK
        verify(g2d, times(1)).stroke = BasicStroke(2f)
        verify(g2d, times(1)).draw(cherry.circle)
        verify(cherry).icon(g2d, DvcsImplIcons.CherryPick)
    }

    fun testPaintCircleHovered() {
        commit = mock(CommitInfo::class.java)
        `when`(commit.isSelected).thenReturn(false)
        `when`(commit.isHovered).thenReturn(true)
        cherry =
            spy(
                CherryCirclePanel(
                    30.0,
                    2f,
                    Palette.BLUE_THEME,
                    commit,
                    next = mock(CirclePanel::class.java),
                    previous = mock(CirclePanel::class.java),
                    isModifiable = true,
                ),
            )
        cherry.paintCircle(g2d)

        assertThat(cherry.colorTheme)
            .isEqualTo(Palette.BLUE_THEME)

        val circleColor = Palette.BLUE_THEME.regularCircleColor
        val borderColor = Palette.BLUE_THEME.borderColor
        verify(cherry).selectedCommitAppearance(g2d, false, circleColor, borderColor)

        verify(g2d, times(1)).color = JBColor.BLACK
        verify(g2d, times(2)).stroke = BasicStroke(2f)
        verify(g2d, times(2)).draw(cherry.circle)
        verify(cherry).icon(g2d, DvcsImplIcons.CherryPick)
    }

    fun testPaintCircleNotModifiable() {
        commit = mock(CommitInfo::class.java)
        `when`(commit.isSelected).thenReturn(true)
        `when`(commit.isHovered).thenReturn(false)
        cherry =
            spy(
                CherryCirclePanel(
                    30.0,
                    2f,
                    Palette.BLUE_THEME,
                    commit,
                    isModifiable = false,
                ),
            )
        cherry.paintCircle(g2d)

        assertThat(cherry.colorTheme)
            .isEqualTo(Palette.GRAY_THEME)

        val circleColor = Palette.GRAY_THEME.regularCircleColor.darker()
        val borderColor = Palette.GRAY_THEME.borderColor.darker()
        verify(cherry).selectedCommitAppearance(g2d, true, circleColor, borderColor)

        verify(g2d, never()).color = JBColor.BLACK
        verify(g2d, times(1)).stroke = BasicStroke(2f)
        verify(g2d, times(1)).draw(cherry.circle)
        verify(cherry).icon(g2d, DvcsImplIcons.CherryPick)
    }
}
