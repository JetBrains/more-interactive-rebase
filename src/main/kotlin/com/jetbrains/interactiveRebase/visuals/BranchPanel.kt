package com.jetbrains.interactiveRebase.visuals

import CirclePanel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import java.awt.*
import javax.swing.Box
import javax.swing.BoxLayout

/**
 * A panel encapsulating a branch:
 * - a number of commits (circle panels)
 * - lines connecting the commits
 */
class BranchPanel(private val commitMessages:
                  List<String>, private val color: JBColor) : JBPanel<JBPanel<*>>() {

    val DIAMETER = 30
    private val borderSize = 1f
    private val size = commitMessages.size

    private val circles: MutableList<CirclePanel> = mutableListOf()

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        preferredSize = Dimension(DIAMETER, (size * DIAMETER * 1.5).toInt())

        for (i in 0 until size) {
            val circle = CirclePanel(DIAMETER.toDouble(), borderSize, color)
            circles.add(circle)
            add(circle)
            if (i < size - 1) {
                add(Box.createVerticalGlue())
            }
        }
    }

    /**
     * Draws the line between the circle nodes.
     */
    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON
        )

        for (i in 0 until size) {
            if (i < size - 1) {
                val circle = circles[i]
                val nextCircle = circles[i + 1]

                // Calculate line coordinates
                val x = (width - DIAMETER) / 2
                val startY = circle.y + DIAMETER / 2
                val endY = nextCircle.y + DIAMETER / 2
                val glueHeight = endY - startY - DIAMETER
                val glueY = startY + DIAMETER / 2 + DIAMETER / 2

                // Make line brush
                g2d.stroke = BasicStroke(borderSize)
                g2d.color = color

                g2d.drawLine(
                    x + DIAMETER / 2,
                    startY,
                    x + DIAMETER / 2,
                    glueY + glueHeight
                )
            }
        }
    }

    /**
     * Getter for the circle panels
     */
    fun getCirclePanels(): MutableList<CirclePanel> {
        return circles
    }
}
