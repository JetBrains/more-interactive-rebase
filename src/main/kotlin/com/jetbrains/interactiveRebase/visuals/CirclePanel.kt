package com.jetbrains.interactiveRebase.visuals

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.listeners.CircleHoverListener
import java.awt.BasicStroke
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D

/**
 * Visual representation of commit node in the git graph
 */
open class CirclePanel(
    val diameter: Double,
    private val border: Float,
    var color: JBColor,
    open var commit: CommitInfo,
    open var next: CirclePanel? = null,
    open var previous: CirclePanel? = null,
) : JBPanel<JBPanel<*>>() {
    var centerX = 0.0
    var centerY = 0.0
    lateinit var circle: Ellipse2D.Double

    /**
     * Makes a panel where the circle will be drawn and
     * sets listeners.
     */

    init {
        isOpaque = false
        minimumSize = Dimension(diameter.toInt(), diameter.toInt())
        createCircle(diameter)
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

        createCircle(diameter)
        val circleColor = if (commit.isSelected) color.darker() else color
        val borderColor = if (commit.isSelected) Palette.BLUEBORDER.darker() else Palette.DARKBLUE
        selectedCommitAppearance(g2d, commit.isSelected, circleColor, borderColor)

        if (commit.isHovered) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
            g2d.color = JBColor.BLACK
            g2d.stroke = BasicStroke(border)
            g2d.draw(circle)
        } else {
            setCursor(Cursor.getDefaultCursor())
        }

        if (commit.isReordered) {
            color = Palette.LIME_GREEN
        }
    }

    /**
     * Creates a circle shape to be drawn inside the panel.
     */
    fun createCircle(diameter: Double) {
        val width = width.toDouble()
        val height = height.toDouble()

        // Calculate the diameter of the circle,
        // so that border is not cropped due to the panel size
        val adjustedDiameter = diameter - 2 * (border + 0.5)

        // Calculate the x and y coordinates for drawing the circle at the center
        val originX = (width - adjustedDiameter) / 2
        val originY = (height - adjustedDiameter) / 2

        centerX = this.x + adjustedDiameter / 2
        centerY = this.y + adjustedDiameter / 2
        circle = Ellipse2D.Double(originX, originY, adjustedDiameter, adjustedDiameter)
    }

    /**
     * Draws the circle with a shadow and border.
     */
    open fun selectedCommitAppearance(
        g2d: Graphics2D,
        isSelected: Boolean,
        circleColor: Color,
        borderColor: Color,
    ) {
        g2d.fill(circle)
        g2d.color = if (isSelected) circleColor.darker() else circleColor
        drawBorder(g2d, circle, borderColor)
    }

    /**
     * Draws the border of the circle.
     */
    open fun drawBorder(
        g2d: Graphics2D,
        circle: Ellipse2D.Double,
        borderColor: Color,
    ) {
        g2d.fill(circle)
        g2d.color = borderColor
        g2d.stroke = BasicStroke(border)
        g2d.draw(circle)
    }

    /**
     * Draws the circle with a shadow and border.
     */
    fun paintSuper(g: Graphics) {
        super.paintComponent(g)
    }
}
