package com.jetbrains.interactiveRebase.visuals

import CirclePanel
import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import java.awt.BasicStroke
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints

class DropCirclePanel(private val diameter: Double,
                      private val border: Float,
                      private val color: JBColor,
                      override var commit: CommitInfo,
                      override var next: CirclePanel? = null,
                      override var previous: CirclePanel? = null,): CirclePanel(diameter, border, color, commit, next, previous) {


    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        // Cast the Graphics object to Graphics2D for more advanced rendering
        val g2d = g as Graphics2D

        // Set rendering hints for smoother rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        createCircle()

        if (commit.isSelected) {
            g2d.color = color.brighter()
        } else {
            g2d.color = color
        }

        g2d.fill(circle)

        if (commit.isHovered) {
            g2d.color = JBColor.BLACK
            g2d.stroke = BasicStroke(border)
            g2d.draw(circle)
        }
    }
}