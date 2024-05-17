package com.jetbrains.interactiveRebase.visuals

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.listeners.CircleHoverListener
import java.awt.*
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
    lateinit var circle: Ellipse2D.Double

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
        paintCircle(g)
    }

    public open fun paintCircle(g: Graphics) {
        val g2d = g as Graphics2D

        // Set rendering hints for smoother rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        createCircle()

        if (commit.isSelected) {
            selectedCommitAppearance(g2d, true,color, Palette.SELECTEDBRIGHT, Palette.BORDER)
        } else {
            selectedCommitAppearance(g2d, false,color,Palette.DARKSHADOW, Palette.DARKBLUE)
        }

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
        val diameter = Math.min(width, height) - 2 * (border + 0.5)

        // Calculate the x and y coordinates for drawing the circle at the center
        val centerX = (width - diameter) / 2
        val centerY = (height - diameter) / 2
        circle = Ellipse2D.Double(centerX, centerY, diameter, diameter)
    }

    open fun selectedCommitAppearance(g2d: Graphics2D, isSelected: Boolean, circleColor: Color, shadowColor: Color, borderColor: Color) {
        drawShadow(g2d, circle, shadowColor)
        g2d.color = if (isSelected) circleColor.darker() else circleColor
        drawBorder(g2d, circle, borderColor)
    }

    open fun drawBorder(g2d: Graphics2D, circle: Ellipse2D.Double, borderColor: Color) {
        g2d.fill(circle)
        g2d.color = borderColor
        g2d.stroke = BasicStroke(border)
        g2d.draw(circle)
    }

    open fun drawShadow(g2d: Graphics2D, circle: Ellipse2D.Double, color: Color) {
        val shadowLayers = 5
        val maxShadowOffset = 3.5
        for (i in 0 until shadowLayers) {
            val alpha = (255 * (1.0 - i.toDouble() / shadowLayers)).toInt() - 20
            val shadowColor = Color(color.red, color.green, color.blue, alpha)
            g2d.color = shadowColor
            val offset = maxShadowOffset * (i.toDouble() / shadowLayers)
            g2d.fill(Ellipse2D.Double(circle.x - offset, circle.y - offset, circle.width + 2 * offset, circle.height + 2 * offset))
        }
        g2d.draw(circle)
        g2d.fill(circle)
    }


    fun paintSuper(g: Graphics){
        super.paintComponent(g)
    }
}
