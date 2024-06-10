package com.jetbrains.interactiveRebase.visuals

import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import icons.VcsLogIcons
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Rectangle2D

class CollapseCirclePanel(
    diameter: Double,
    private val border: Float,
    color: JBColor,
    override var commit: CommitInfo,
    override var next: CirclePanel? = null,
    override var previous: CirclePanel? = null,
) :
    CirclePanel(diameter, border, color, commit, next, previous) {
    lateinit var rectangle: Rectangle2D

    override fun paintCircle(g: Graphics) {
        val g2d = g as Graphics2D

        // Set rendering hints for smoother rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        createCircle(diameter)

        val elementColor = parent.background

        createRectangle(diameter + 2)
        g2d.fill(rectangle)
        g2d.color = elementColor
        g2d.fill(rectangle)
        g2d.color = elementColor
        g2d.draw(rectangle)

        if (commit.isHovered) {
            g2d.color = elementColor.darker()
            g2d.fill(rectangle)
            g2d.draw(rectangle)
        }

        paintIcon(g2d)
    }

    fun createRectangle(diameter: Double) {
        val width = width.toDouble()
        val height = height.toDouble()

        val adjustedHeight = diameter - 2 * (border + 0.5)
        val adjustedWidth = adjustedHeight / 1.5

        val originX = (width - adjustedWidth) / 2
        val originY = (height - adjustedHeight) / 2

        centerX = this.x + adjustedWidth / 2
        centerY = this.y + adjustedHeight / 2
        rectangle = Rectangle2D.Double(originX, originY, adjustedWidth, adjustedHeight)
    }

    fun paintIcon(g: Graphics) {
        val icon = RotatedIcon(VcsLogIcons.Process.Dots_1, 90.0)
        val iconX = (width - icon.iconWidth) / 2
        val iconY = (height - icon.iconHeight) / 2
        icon.paintIcon(this, g, iconX, iconY)
    }
}
