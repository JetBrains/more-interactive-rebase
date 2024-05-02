package com.jetbrains.interactiveRebase.components

import com.intellij.ui.components.JBPanel
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

/**
 * Visual representation of commit node in the git graph
 */
class Node : JBPanel<JBPanel<*>>() {

    // Flag to track whether the mouse is currently hovering over the panel
    private var isHovering = false

    init {
        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                super.mouseEntered(e)
                isHovering = true
                repaint()
            }

            override fun mouseExited(e: MouseEvent?) {
                super.mouseExited(e)
                isHovering = false
                repaint()
            }
        })
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        // Cast the Graphics object to Graphics2D for more advanced rendering
        val g2d = g as Graphics2D

        // Set rendering hints for smoother rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Set the color of the circle
        g2d.color = Color.BLUE

        // Get the width and height of the panel
        val width = width
        val height = height

        // Calculate the diameter of the circle
        val diameter = Math.min(width, height)

        // Calculate the x and y coordinates for drawing the circle at the center
        val x = (width - diameter) / 2
        val y = (height - diameter)/ 2

        // Draw the circle
        g2d.fillOval(x, y, diameter, diameter)

        // Draw white outline if hovering
        if (isHovering) {
            g2d.color = Color.WHITE
            g2d.stroke = BasicStroke(2f)
            g2d.drawOval(x, y, diameter, diameter)
        }
    }
}
