package com.jetbrains.interactiveRebase.visuals

import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import java.awt.*
import java.awt.geom.Ellipse2D

class DropCirclePanel(private val diameter: Double,
                      private val border: Float,
                      private val color: JBColor,
                      override var commit: CommitInfo,
                      override var next: CirclePanel? = null,
                      override var previous: CirclePanel? = null,): CirclePanel(diameter, border, color, commit, next, previous) {


    override fun paintCircle(g: Graphics) {
        val g2d = g as Graphics2D
        g2d.color = JBColor.DARK_GRAY
        // Set rendering hints for smoother rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        createCircle()
        g2d.fill(circle)

        g2d.stroke = BasicStroke(border)
        val shadowColor = Color(0, 0, 0, 50)
        val shadowOffset = 5

        val borderColor = Color.BLACK

        circle.let {
            g2d.color = shadowColor
            g2d.fill(Ellipse2D.Double(it.x , it.y, it.width + shadowOffset, it.height + shadowOffset))

            // Draw circle
            g2d.fill(it)

            // Draw dotted border
            val dashPattern = floatArrayOf(3f, 3f)
            g2d.stroke = BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dashPattern, 0f)
            g2d.color = borderColor
            g2d.draw(it)
        }
    }
}