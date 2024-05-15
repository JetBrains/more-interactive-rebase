package com.jetbrains.interactiveRebase.visuals

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.listeners.CircleHoverListener
import java.awt.BasicStroke
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D

/**
 * Visual representation of commit node in the git graph
 */
open class CirclePanel(
        private val diameter: Double,
        private val border: Float,
        private val color: JBColor,
        open var commit: CommitInfo,
        open var next: CirclePanel? = null,
        open var previous: CirclePanel? = null,
) : JBPanel<JBPanel<*>>() {
    private var centerX = 0.0
    private var centerY = 0.0
    lateinit var circle: Ellipse2D

    /**
     * Makes a panel where the circle will be drawn and
     * sets listeners.
     */

    init {
        isOpaque = false
        preferredSize = minimumSize
        createCircle()
        addMouseListener(CircleHoverListener(this))
        addMouseMotionListener(CircleHoverListener(this))
    }

    /**
     * Draws circles within the circle panel
     * - if hovered put an outline
     * - if selected make color darker
     */

    public override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        // Cast the Graphics object to Graphics2D for more advanced rendering
        val g2d = g as Graphics2D

        // Set rendering hints for smoother rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        createCircle()

        if (commit.isSelected) {
            g2d.color = color.darker()
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

    /**
     * Creates a circle shape to be drawn inside the panel.
     */
    fun createCircle() {
        val width = width.toDouble()
        val height = height.toDouble()

        // Calculate the diameter of the circle,
        // so that border is not cropped due to the panel size
        val diameter = diameter - 2 * border

        // Calculate the x and y coordinates for drawing the circle at the center
        centerX = (width - diameter) / 2
        centerY = (height - diameter) / 2
        circle = Ellipse2D.Double(centerX, centerY, diameter, diameter)
    }
}
