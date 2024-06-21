package com.jetbrains.interactiveRebase.visuals

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
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
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Rectangle2D

class CollapseCirclePanelTest : BasePlatformTestCase() {
    init {
        System.setProperty("idea.home.path", "/tmp")
    }

    private lateinit var collapseCirclePanel: CollapseCirclePanel
    private lateinit var commit: CommitInfo
    private lateinit var g: Graphics2D

    @Captor
    private lateinit var colorCaptor: ArgumentCaptor<Color>

    override fun setUp() {
        super.setUp()
        val commitProvider = TestGitCommitProvider(project)
        commit = CommitInfo(commitProvider.createCommit("LOL"), project, mutableListOf())
        g = mock(Graphics2D::class.java)

        collapseCirclePanel =
            spy(
                CollapseCirclePanel(
                    50.0,
                    2.0f,
                    Palette.BLUE_THEME,
                    commit,
                ),
            )
        openMocks(this)

        doAnswer {
            commit
        }.`when`(collapseCirclePanel).commit
        val parent = JBPanel<JBPanel<*>>()
        parent.background = JBColor.WHITE

        doAnswer {
            parent
        }.`when`(collapseCirclePanel).parent
    }

    fun testPaintCircleSelected() {
        collapseCirclePanel =
            spy(
                CollapseCirclePanel(
                    50.0,
                    2.0f,
                    Palette.BLUE_THEME,
                    commit,
                    mock(CirclePanel::class.java),
                ),
            )
        doAnswer {
            commit
        }.`when`(collapseCirclePanel).commit
        val parent = JBPanel<JBPanel<*>>()
        parent.background = JBColor.WHITE

        doAnswer {
            parent
        }.`when`(collapseCirclePanel).parent
        commit.isSelected = true
        collapseCirclePanel.paintCircle(g)
        verify(g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(g, times(2)).fill(any(Rectangle2D.Double::class.java))
        verify(g, times(1)).draw(any(Rectangle2D.Double::class.java))

        verify(g, times(2)).color = colorCaptor.capture()

        assertEquals(2, colorCaptor.allValues.size)
        assertEquals(colorCaptor.allValues[0], JBColor.WHITE)
        assertEquals(colorCaptor.allValues[1], JBColor.WHITE)
    }

    fun testPaintCircleHovered() {
        collapseCirclePanel =
            spy(
                CollapseCirclePanel(
                    50.0,
                    2.0f,
                    Palette.BLUE_THEME,
                    commit,
                    previous = mock(CirclePanel::class.java),
                ),
            )
        doAnswer {
            commit
        }.`when`(collapseCirclePanel).commit
        val parent = JBPanel<JBPanel<*>>()
        parent.background = JBColor.WHITE

        doAnswer {
            parent
        }.`when`(collapseCirclePanel).parent
        commit.isHovered = true
        collapseCirclePanel.paintCircle(g)

        verify(g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(g, times(3)).fill(any(Rectangle2D.Double::class.java))
        verify(g, times(2)).draw(any(Rectangle2D.Double::class.java))
        verify(g, times(3)).color = colorCaptor.capture()

        assertEquals(3, colorCaptor.allValues.size)
        assertEquals(JBColor.WHITE, colorCaptor.allValues[0])
        assertEquals(JBColor.WHITE, colorCaptor.allValues[1])
        assertEquals(JBColor.WHITE.darker(), colorCaptor.allValues[2])
    }
}
