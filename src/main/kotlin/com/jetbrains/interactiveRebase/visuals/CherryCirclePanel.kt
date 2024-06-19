package com.jetbrains.interactiveRebase.visuals

import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import java.awt.BasicStroke
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints

class CherryCirclePanel(
    diameter: Double,
    private val border: Float,
    override var colorTheme: Palette.Theme,
    override var commit: CommitInfo,
    override var next: CirclePanel? = null,
    override var previous: CirclePanel? = null,
    private val isModifiable: Boolean = true,
) : CirclePanel(diameter, border, colorTheme, commit, next, previous) {
    /**
     * Draws a circle with a cherry inside
     */
    override fun paintCircle(g: Graphics) {
        val g2d = g as Graphics2D

        // Set rendering hints for smoother rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        createCircle(diameter)
        if (!isModifiable) {
            colorTheme = Palette.GRAY_THEME
        }
        val circleColor =
            if (commit.isSelected) {
                colorTheme.regularCircleColor.darker()
            } else {
                colorTheme.regularCircleColor
            }
        val borderColor =
            if (commit.isSelected) {
                colorTheme.borderColor.darker() as JBColor
            } else {
                colorTheme.borderColor
            }
        selectedCommitAppearance(g2d, commit.isSelected, circleColor, borderColor)

        if (commit.isHovered) {
            g2d.color = JBColor.BLACK
            g2d.stroke = BasicStroke(border)
            g2d.draw(circle)
        }

        paintCherry(g2d)
    }
}
