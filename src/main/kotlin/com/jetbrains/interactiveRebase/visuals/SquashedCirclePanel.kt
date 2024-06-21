package com.jetbrains.interactiveRebase.visuals

import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D

open class SquashedCirclePanel(
    diameter: Double,
    private val border: Float,
    colorTheme: Palette.Theme,
    override var commit: CommitInfo,
    override var next: CirclePanel? = null,
    override var previous: CirclePanel? = null,
) : CirclePanel(diameter * 1.7, border, colorTheme, commit, next, previous) {
    lateinit var backCircle: Ellipse2D.Double

    /**
     * Paints a squashed circle
     * panel
     */
    override fun paintCircle(g: Graphics) {
        val g2d = g as Graphics2D

        // Set rendering hints for smoother rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        createCircle(diameter)
        val (circleColor, borderColor) = colorCircle()
        selectedCommitAppearance(g2d, commit.isSelected, circleColor, borderColor)

        if (commit.isHovered) {
            g2d.color = JBColor.BLACK
            g2d.stroke = BasicStroke(border)
            g2d.draw(circle)
        }
    }

    /**
     * Creates a circle shape to be drawn inside the panel.
     */
    override fun createCircle(diameter: Double) {
        val width = width.toDouble()
        val height = height.toDouble()

        // Calculate the diameter of the circle,
        // so that border is not cropped due to the panel size
        val adjustedDiameter = diameter / 1.6 - 2 * (border + 0.5)

        // Calculate the x and y coordinates for drawing the circle at the center
        val originX = (width - adjustedDiameter) / 2
        val originY = (height - adjustedDiameter) / 2

        centerX = this.x + adjustedDiameter / 2
        centerY = this.y + adjustedDiameter / 2
        backCircle =
            Ellipse2D.Double(
                originX + adjustedDiameter * 0.35,
                originY - adjustedDiameter * 0.35,
                adjustedDiameter * 0.9,
                adjustedDiameter * 0.9,
            )
        circle = Ellipse2D.Double(originX, originY, adjustedDiameter, adjustedDiameter)
    }

    /**
     * Draws the circle with a shadow and border.
     */
    override fun selectedCommitAppearance(
        g2d: Graphics2D,
        isSelected: Boolean,
        circleColor: Color,
        borderColor: Color,
    ) {
        g2d.fill(backCircle)
        g2d.fill(circle)
        g2d.color = if (isSelected) circleColor.darker() else circleColor
        drawBorder(g2d, backCircle, borderColor)
        g2d.color = if (isSelected) circleColor.darker() else circleColor
        drawBorder(g2d, circle, borderColor)
    }
}
