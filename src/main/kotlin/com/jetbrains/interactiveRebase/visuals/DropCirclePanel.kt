package com.jetbrains.interactiveRebase.visuals

import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D

class DropCirclePanel(
    diameter: Double,
    private val border: Float,
    color: JBColor,
    override var commit: CommitInfo,
    override var next: CirclePanel? = null,
    override var previous: CirclePanel? = null,
) : CirclePanel(diameter, border, color, commit, next, previous) {
    /**
     * Draws a circle with a dashed border
     */
    override fun paintCircle(g: Graphics) {
        val g2d = g as Graphics2D
        // Set rendering hints for smoother rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        createCircle(diameter)
        val circleColor = if (commit.isSelected) Palette.GRAY.darker() else Palette.GRAY
        val borderColor = if (commit.isSelected) Palette.BLUEBORDER.darker() else Palette.BLUEBORDER

        selectedCommitAppearance(g2d, commit.isSelected, circleColor, borderColor)
        if (commit.isHovered) {
            g2d.color = JBColor.BLACK
            g2d.stroke = BasicStroke(border)
            g2d.draw(circle)
        }
    }

    /**
     * Draws a dashed border around the circle
     */
    override fun drawBorder(
        g2d: Graphics2D,
        circle: Ellipse2D.Double,
        borderColor: Color,
    ) {
        g2d.fill(circle)
        g2d.color = borderColor

        val dashPattern = floatArrayOf(3f, 3f)
        g2d.stroke = BasicStroke(1.5f * border, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dashPattern, 0f)
        g2d.draw(circle)
    }
}
