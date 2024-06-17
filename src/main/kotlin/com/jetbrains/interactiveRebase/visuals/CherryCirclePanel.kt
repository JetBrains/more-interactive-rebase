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
    colorTheme: Palette.Theme,
    override var commit: CommitInfo,
    override var next: CirclePanel? = null,
    override var previous: CirclePanel? = null,
) : CirclePanel(diameter, border, colorTheme, commit, next, previous) {
    /**
     * Draws a circle with a cherry inside
     */
    override fun paintCircle(g: Graphics) {
        val g2d = g as Graphics2D

        // Set rendering hints for smoother rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        createCircle(diameter)
        val circleColor = if (commit.isSelected) Palette.DARK_GRAY.darker() else Palette.JETBRAINS_GRAY
        val borderColor = if (commit.isSelected) colorTheme.borderColor.darker() else colorTheme.borderColor
        selectedCommitAppearance(g2d, commit.isSelected, circleColor, borderColor)

        if (commit.isHovered) {
            g2d.color = JBColor.BLACK
            g2d.stroke = BasicStroke(border)
            g2d.draw(circle)
        }

        paintCherry(g2d)
    }
}