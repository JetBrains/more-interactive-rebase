package com.jetbrains.interactiveRebase.visuals

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.StopToEditCommand
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
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
) : CirclePanel(diameter * 1.6, border, colorTheme, commit, next, previous) {
    lateinit var backCircle: Ellipse2D.Double
    lateinit var middleCircle: Ellipse2D.Double

    init {
        minimumSize =
            Dimension(
                JBUI.scale((diameter * 1.6).toInt()),
                // Ensure there is always spacing between circles
                // when drawing a branch
                JBUI.scale((diameter.toInt() * 2.0).toInt()),
            )
        preferredSize = minimumSize
    }

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
            drawWhiteOutlineAroundTheThreeCircles(g2d, circleColor)
        }

        if (commit.getChangesAfterPick().any { it is StopToEditCommand }) {
            paintPauseInsideSquash(g2d, circle)
        }
    }

    private fun drawWhiteOutlineAroundTheThreeCircles(
        g2d: Graphics2D,
        circleColor: Color,
    ) {
        g2d.stroke = BasicStroke(border)

        g2d.color = JBColor.BLACK
        g2d.draw(backCircle)
        g2d.color = circleColor
        g2d.fill(middleCircle)

        g2d.color = JBColor.BLACK
        g2d.draw(middleCircle)
        g2d.color = circleColor
        g2d.fill(circle)

        g2d.color = JBColor.BLACK
        g2d.draw(circle)
    }

    internal fun paintPauseInsideSquash(
        g2d: Graphics2D,
        ellipse: Ellipse2D,
    ) {
        val icon = AllIcons.Actions.Pause
        val iconX = ellipse.x + (ellipse.width - icon.iconWidth) / 2
        val iconY = ellipse.y + (ellipse.height - icon.iconHeight) / 2
        icon.paintIcon(this, g2d, iconX.toInt(), iconY.toInt())
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
        middleCircle =
            Ellipse2D.Double(
                originX + adjustedDiameter * 0.25,
                originY - adjustedDiameter * 0.2,
                adjustedDiameter * 0.9,
                adjustedDiameter * 0.9,
            )
        backCircle =
            Ellipse2D.Double(
                originX + adjustedDiameter * 0.45,
                originY - adjustedDiameter * 0.35,
                adjustedDiameter * 0.8,
                adjustedDiameter * 0.8,
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
        g2d.color = if (isSelected) circleColor.darker() else Palette.GRAY
        val borderStyle = if (isSelected) borderColor else parent.background
        drawBorder(g2d, backCircle, borderStyle)
        val middleCircleColor = interpolateColors(Palette.GRAY, circleColor)
        g2d.color = if (isSelected) circleColor.darker() else middleCircleColor
        drawBorder(g2d, middleCircle, borderStyle)
        g2d.color = if (isSelected) circleColor.darker() else circleColor
        drawBorder(g2d, circle, borderStyle)
    }

    /**
     * Interpolates between two colors.
     *
     * @param color1 the first color
     * @param color2 the second color
     * @param fraction the fraction between 0 and 1 to interpolate
     * @return the interpolated color
     */
    internal fun interpolateColors(
        color1: Color,
        color2: Color,
        fraction: Float = 0.5f,
    ): Color {
        val red = (color1.red + (color2.red - color1.red) * fraction).toInt()
        val green = (color1.green + (color2.green - color1.green) * fraction).toInt()
        val blue = (color1.blue + (color2.blue - color1.blue) * fraction).toInt()
        return Color(red, green, blue)
    }
}
