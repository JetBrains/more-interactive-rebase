//package com.jetbrains.interactiveRebase.visuals
//
//import com.intellij.testFramework.fixtures.BasePlatformTestCase
//import com.intellij.ui.JBColor
//import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
//import git4idea.GitCommit
//import org.assertj.core.api.Assertions.assertThat
//import org.junit.jupiter.api.Test
//
//import org.junit.jupiter.api.Assertions.*
//import org.mockito.Mockito.*
//import java.awt.BasicStroke
//import java.awt.Graphics
//import java.awt.Graphics2D
//import javax.swing.plaf.ComponentUI
//
//class CherryCirclePanelTest: BasePlatformTestCase() {
//
//    private lateinit var cherry: CherryCirclePanel
//    private lateinit var commit: CommitInfo
//    private lateinit var g2d: Graphics2D
//
//    override fun setUp() {
//        super.setUp()
//        val g: Graphics = mock(Graphics::class.java)
//        g2d = mock(Graphics2D::class.java)
//        `when`(g2d.create()).thenReturn(g)
//    }
//
//    fun paintCircle() {
//
//        commit = mock(CommitInfo::class.java)
//        `when`(commit.isSelected).thenReturn(false)
//        `when`(commit.isHovered).thenReturn(false)
//        cherry = spy(CherryCirclePanel(
//            30.0,
//            2f,
//            Palette.BLUE_THEME,
//            commit,
//            isModifiable = true,
//        ))
//        cherry.paintCherry(g2d)
//
//        assertThat(cherry.colorTheme)
//            .isEqualTo(Palette.BLUE_THEME)
//
//        val circleColor = Palette.BLUE_THEME.regularCircleColor
//        val borderColor = Palette.BLUE_THEME.borderColor
//        verify(cherry).
//            selectedCommitAppearance(g2d, false, circleColor, borderColor)
//
//        verify(g2d, never()).color = JBColor.BLACK
//                verify(g2d, never()).stroke = BasicStroke(2)
//                verify(g2d, never()).draw(cherry)
//        verify(cherry).paintCherry(g2d)
//    }
//}