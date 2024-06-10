package com.jetbrains.interactiveRebase.visuals

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.CollapseCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.FixupCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.StopToEditCommand
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.LinearGradientPaint
import java.awt.RenderingHints

/**
 * A panel encapsulating a branch:
 * - a number of commits (circle panels)
 * - lines connecting the commits
 */
class BranchPanel(
    private val branch: BranchInfo,
    val color: JBColor,
) : JBPanel<JBPanel<*>>() {
    val diameter = 30
    val borderSize = 1f
    private var size = branch.currentCommits.size

    val circles: MutableList<CirclePanel> = mutableListOf()

    /**
     * Makes a branch panel with vertical orientation
     * - sets dimensions
     * - adds commits to the branch (circle panel)
     */
    init {
        layout = GridBagLayout()

        updateCommits()
    }

    /**
     * Makes a circle panel and links it
     * to the next and previous neighbors
     */
    fun initializeCirclePanel(i: Int): CirclePanel {
        val commit = branch.currentCommits[i]
        var circle =
            CirclePanel(
                diameter.toDouble(),
                borderSize,
                color,
                branch.currentCommits[i],
            )

        val visualChanges = commit.getChangesAfterPick()

        if (visualChanges.any { it is CollapseCommand }) {
            circle =
                CollapseCirclePanel(
                    diameter.toDouble(),
                    4f,
                    color,
                    branch.currentCommits[i],
                )
        } else if (visualChanges.any { it is DropCommand }) {
            circle =
                DropCirclePanel(
                    (diameter + 2).toDouble(),
                    borderSize,
                    color,
                    branch.currentCommits[i],
                )
        } else if (visualChanges.any { it is StopToEditCommand }) {
            circle =
                StopToEditCirclePanel(
                    diameter.toDouble(),
                    borderSize,
                    color,
                    branch.currentCommits[i],
                )
        } else if (visualChanges.any { it is SquashCommand } || visualChanges.any { it is FixupCommand }) {
            circle =
                SquashedCirclePanel(
                    diameter.toDouble(),
                    borderSize,
                    color,
                    branch.currentCommits[i],
                )
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

        for (i in 0 until size - 1) {
            // Make line brush
            g2d.stroke = BasicStroke(borderSize)
            g2d.color = color
            drawLineBetweenCommits(i, g2d)
        }

        drawBottomLine(g2d)
    }

    /**
     * Draws the line from the earliest branch commit
     * to the bottom of the screen
     */
    internal fun drawBottomLine(g2d: Graphics2D) {
        val startX = width / 2
        val startY = circles[size - 1].y + circles[size - 1].height / 2 + diameter / 2
        val endX = width / 2
        val endY = y + height

        fadingAwayEffect(g2d, startX, startY, endX, endY)

        g2d.drawLine(startX, startY, endX, endY)
    }

    /**
     * Adds a gradient to the line
     * to make it seem as if it's fading away
     */
    internal fun fadingAwayEffect(
        g2d: Graphics2D,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
    ) {
        val fractions = floatArrayOf(0.0f, 0.5f)
        val colors = arrayOf<Color>(color, JBColor.PanelBackground)

        g2d.paint =
            LinearGradientPaint(
                startX.toFloat(),
                startY.toFloat(),
                endX.toFloat(),
                endY.toFloat(),
                fractions,
                colors,
            )
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
        val x = width / 2
        val startY = circle.y + circle.height / 2 + diameter / 2
        val endY = nextCircle.y + circle.height / 2 + diameter / 2
        val glueHeight = endY - startY - diameter
        val glueY = startY + diameter / 2 + diameter / 2

        g2d.color = color
        g2d.stroke = BasicStroke(2f)
        g2d.drawLine(
            x,
            startY,
            x,
            glueY + glueHeight,
        )

        // Make line thicker
//        val shadowOffset = 1
//        g2d.color = color
//        g2d.drawLine(
//            x + diameter / 2 + shadowOffset,
//            startY + shadowOffset,
//            x + diameter / 2 + shadowOffset,
//            glueY + glueHeight + shadowOffset,
//        )
    }

    /**
     * Sets commits to be shown in branch
     */

    fun updateCommits() {
        removeAll()
        circles.clear()

        size = branch.currentCommits.size

        for (i in 0 until size) {
            val circle = initializeCirclePanel(i)

            val gbc = GridBagConstraints()
            gbc.gridx = 0
            gbc.gridy = i
            gbc.weightx = 0.0
            gbc.weighty = if (i == size - 1) 1.0 else 0.0
            gbc.anchor = GridBagConstraints.NORTH
            gbc.fill = GridBagConstraints.HORIZONTAL
            add(circle, gbc)
        }
        revalidate()
    }

    fun prepareCommitsForCollapsing() {
        if (branch.currentCommits.size < 7) return
        val sizey = branch.currentCommits.size
        val newCurrentCommits = branch.currentCommits.subList(0, 5) + branch.currentCommits.subList(sizey - 2, sizey)

        val collapsedCommits = branch.currentCommits.subList(5, sizey - 2)
        val parentOfCollapsedCommit = branch.currentCommits[sizey - 2]

        val collapsedCommand = CollapseCommand(parentOfCollapsedCommit, collapsedCommits.toMutableList())

        parentOfCollapsedCommit.changes.add(collapsedCommand)
        parentOfCollapsedCommit.isCollapsed = true

        collapsedCommits.forEach {
            it.changes.add(collapsedCommand)
            it.isCollapsed = true
        }
        branch.currentCommits = newCurrentCommits.toMutableList()
    }
}
