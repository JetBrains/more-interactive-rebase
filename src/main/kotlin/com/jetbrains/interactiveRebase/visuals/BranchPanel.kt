package com.jetbrains.interactiveRebase.visuals

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.StopToEditCommand
import java.awt.BasicStroke
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.Box
import javax.swing.BoxLayout

/**
 * A panel encapsulating a branch:
 * - a number of commits (circle panels)
 * - lines connecting the commits
 */
class BranchPanel(
    private val branch: BranchInfo,
    val color: JBColor,
) : JBPanel<JBPanel<*>>() {
    val diameter = 25
    val borderSize = 1f
    private var size = branch.commits.size

    val circles: MutableList<CirclePanel> = mutableListOf()

    /**
     * Makes a branch panel with vertical orientation
     * - sets dimensions
     * - adds commits to the branch (circle panel)
     */
    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        preferredSize = Dimension(diameter, (size * diameter * 1.5).toInt())

        updateCommits()
    }

    /**
     * Makes a circle panel and links it
     * to the next and previous neighbors
     */
    fun initializeCirclePanel(i: Int): CirclePanel {
        val commit = branch.commits[i]
        var circle = CirclePanel(diameter.toDouble(), borderSize, color, branch.commits[i])

        if (commit.changes.any { it is DropCommand } == true) {
            circle = DropCirclePanel(diameter.toDouble(), borderSize, color, branch.commits[i])
        }

        if (commit.changes.any { it is StopToEditCommand } == true) {
            circle = StopToEditCirclePanel(diameter.toDouble(), borderSize, color, branch.commits[i])
        }

        circles.add(circle)
        if (i > 0) {
            // Set reference to next circle
            circles[i - 1].next = circle
            // Set reference to previous circle
            circle.previous = circles[i - 1]
        }
        return circle
    }

    /**
     * Draws the line between the circle nodes.
     */
    public override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON,
        )

        for (i in 0 until size) {
            if (i < size - 1) {
                // Make line brush
                g2d.stroke = BasicStroke(borderSize)
                g2d.color = color
                drawLineBetweenCommits(i, g2d)
            }
        }
    }

    /**
     * Draws a line between two subsequent commits
     * by calculating the start and end position
     * of the line
     */
    private fun drawLineBetweenCommits(
        i: Int,
        g2d: Graphics2D,
    ) {
        val circle = circles[i]
        val nextCircle = circles[i + 1]

        // Calculate line coordinates
        val x = (width - diameter) / 2
        val startY = circle.y + diameter / 2
        val endY = nextCircle.y + diameter / 2
        val glueHeight = endY - startY - diameter
        val glueY = startY + diameter / 2 + diameter / 2

        g2d.color = color
        g2d.drawLine(
            x + diameter / 2,
            startY,
            x + diameter / 2,
            glueY + glueHeight,
        )

        // Make line thicker
        val shadowOffset = 1
        g2d.color = color
        g2d.drawLine(
            x + diameter / 2 + shadowOffset,
            startY + shadowOffset,
            x + diameter / 2 + shadowOffset,
            glueY + glueHeight + shadowOffset,
        )
    }

    /**
     * Getter for the circle panels.
     */
    fun getCirclePanels(): MutableList<CirclePanel> {
        return circles
    }

    /**
     * Sets commits to be shown in branch
     */

    fun updateCommits() {
        removeAll()
        circles.clear()

        size = branch.commits.size

        for (i in 0 until size) {
            val circle = initializeCirclePanel(i)
            add(circle)
            if (i < size - 1) {
                add(Box.createVerticalGlue())
            }
        }
        super.revalidate()
        revalidate()
    }
}
