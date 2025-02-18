package com.jetbrains.interactiveRebase.visuals

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.CherryCommand
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
    val branch: BranchInfo,
    var colorTheme: Palette.Theme,
) : JBPanel<JBPanel<*>>() {
    val diameter = 30
    val borderSize = 1f
    private var size = branch.currentCommits.size

    var circles: MutableList<CirclePanel> = mutableListOf()

    /**
     * Makes a branch panel with vertical orientation
     * - sets dimensions
     * - adds commits to the branch (circle panel)
     */
    init {
        layout = GridBagLayout()
        isOpaque = false
        updateCommits()
    }

    /**
     * Makes a circle panel and links it
     * to the next and previous neighbors
     */
    fun initializeCirclePanel(i: Int): CirclePanel {
        val commit = branch.currentCommits[i]
        var theme = colorTheme
        if (commit.isRebased) {
            theme = Palette.LIME_GREEN_THEME
        } else if (commit.isPaused) {
            theme = Palette.LIME_THEME
        }

        var circle =
            CirclePanel(
                diameter.toDouble(),
                borderSize,
                theme,
                branch.currentCommits[i],
            )

        val visualChanges = commit.getChangesAfterPick()

        if (visualChanges.any { it is CollapseCommand }) {
            circle =
                CollapseCirclePanel(
                    diameter.toDouble(),
                    4f,
                    theme,
                    branch.currentCommits[i],
                )
        } else if (visualChanges.any { it is DropCommand }) {
            circle =
                DropCirclePanel(
                    (diameter + 2).toDouble(),
                    borderSize,
                    theme,
                    branch.currentCommits[i],
                )
        } else if (visualChanges.any { it is SquashCommand } || visualChanges.any { it is FixupCommand }) {
            circle =
                SquashedCirclePanel(
                    diameter.toDouble(),
                    borderSize,
                    theme,
                    branch.currentCommits[i],
                )
        } else if (visualChanges.any { it is StopToEditCommand }) {
            circle =
                StopToEditCirclePanel(
                    diameter.toDouble(),
                    borderSize,
                    theme,
                    branch.currentCommits[i],
                )
        } else if (visualChanges.any { it is CherryCommand }) {
            circle =
                CherryCirclePanel(
                    diameter.toDouble(),
                    borderSize,
                    theme,
                    branch.currentCommits[i],
                    isModifiable = branch.isWritable,
                )
        } else if (commit.wasCherryPicked) {
            circle =
                CherryCirclePanel(
                    diameter.toDouble(),
                    borderSize,
                    colorTheme,
                    commit,
                    isModifiable = branch.isWritable,
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
            g2d.color = circles[i].colorTheme.regularCircleColor
            drawLineBetweenCommits(i, g2d)
        }

        if (!branch.isPrimary && circles.isNotEmpty()) {
            drawBottomLine(g2d)
        }
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

        val colors = arrayOf<Color>(circles[circles.size - 1].colorTheme.regularCircleColor, JBColor.PanelBackground)

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
        val startY = circle.y + circle.height / 2
        var endY = nextCircle.y + circle.height / 2

        val fractions = floatArrayOf(0.2f, 0.8f)
        val colors =
            arrayOf<Color>(
                circle.colorTheme.regularCircleColor,
                nextCircle.colorTheme.regularCircleColor,
            )

        if (startY == endY) {
            endY += 5
        }
        g2d.paint =
            LinearGradientPaint(
                x.toFloat(),
                startY.toFloat(),
                x.toFloat(),
                endY.toFloat(),
                fractions,
                colors,
            )

        g2d.stroke = BasicStroke(2f)
        g2d.drawLine(
            x,
            startY,
            x,
            endY,
        )
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

            if (i == 0) {
                gbc.insets.top = diameter
            }
            if (i == size - 1) {
                gbc.insets.bottom = diameter
            }
            add(circle, gbc)
        }
        revalidate()
    }
}
