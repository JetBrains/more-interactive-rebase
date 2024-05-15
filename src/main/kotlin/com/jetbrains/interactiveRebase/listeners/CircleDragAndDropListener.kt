package com.jetbrains.interactiveRebase.listeners

import CirclePanel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.util.preferredHeight
import com.intellij.util.ui.UIUtil
import com.jetbrains.interactiveRebase.visuals.LabeledBranchPanel
import com.jetbrains.interactiveRebase.visuals.Palette
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import kotlin.math.abs

class CircleDragAndDropListener(
    private val circle: CirclePanel,
    private val circles: MutableList<CirclePanel>,
    private val parent: LabeledBranchPanel,
) : MouseAdapter() {
    private var labels = parent.commitLabels
    private var label = getLabel(circle, labels)
    private var currCirclePos = Point(circle.x, circle.y)
    private var circlesPositions = mutableListOf<CirclePosition>()
    private var labelsPositions = mutableListOf<Point>()
    private val initialCircleColor = circle.color
    private val initialFontColor = label.fontColor
    private var hasMoved = false
    private val minY = 0
    private var maxY = parent.branchPanel.preferredHeight - circle.diameter.toInt()

    /**
     * NEW: UPDATES CommitInfo
     */
    private val commit = circle.commit
    private val commits = parent.branch.currentCommits
    private var currentIndex = commits.indexOf(commit)

    init {
        SwingUtilities.invokeLater {
            maxY = parent.branchPanel.preferredHeight - circle.height
        }
    }

    override fun mousePressed(e: MouseEvent) {
        hasMoved = false
        currCirclePos.x = e.xOnScreen
        currCirclePos.y = e.yOnScreen
        circlesPositions =
            circles.map { c ->
                CirclePosition(c.centerX.toInt(), c.centerY.toInt(), c.x, c.y)
            }.toMutableList()
        labelsPositions =
            labels.map { l ->
                Point(l.x, l.y)
            }.toMutableList()
    }

    override fun mouseDragged(e: MouseEvent) {
        hasMoved = true
        circle.color = Palette.TOMATO
        label.fontColor = UIUtil.FontColor.BRIGHTER
        val deltaY = e.yOnScreen - currCirclePos.y
        val newCircleY = circle.y + deltaY

        // Check if the new position exceeds the upper or lower limit
        val newCircleYBounded = newCircleY.coerceIn(minY, maxY)

        // Update the circle and label positions
        circle.setLocation(circle.x, newCircleYBounded)
        label.setLocation(label.x, newCircleYBounded)

        currCirclePos.x = e.xOnScreen
        currCirclePos.y = e.yOnScreen

        val newIndex = findNewPosition()
        if (newIndex != currentIndex) {
            updateIndices(newIndex, currentIndex)
            repositionOnDrag()
            parent.repaint()
            currentIndex = newIndex
        }

        // Handle visual indication of movement limits
        indicateLimitedVerticalMovement(newCircleY)
    }

    override fun mouseReleased(e: MouseEvent) {
        if (hasMoved) {
            circle.color = Palette.LIME_GREEN
            label.fontColor = initialFontColor
            repositionOnDrop()
            parent.repaint()
        }
    }

    private fun updateIndices(
        newIndex: Int,
        oldIndex: Int,
    ) {
        circles.removeAt(oldIndex)
        circles.add(newIndex, circle)
        labels.removeAt(oldIndex)
        labels.add(newIndex, label)

        /**
         * NEW: Updates CommitInfo
         */
        commits.removeAt(oldIndex)
        commits.add(newIndex, commit)

        updateNeighbors(oldIndex)
        updateNeighbors(newIndex)
    }

    private fun updateNeighbors(index: Int) {
        if (index < circles.size - 1) {
            circles[index].next = circles[index + 1]
        } else {
            // Set next to null for the last circle
            circles[index].next = null
        }
        if (index > 0) {
            circles[index].previous = circles[index - 1]
        } else {
            // Set previous to null for the first circle
            circles[index].previous = null
        }
    }

    private fun findNewPosition(): Int {
        var newIndex = -1
        var closestDistance = Int.MAX_VALUE
        val newY = circle.y

        for ((index, pos) in circlesPositions.withIndex()) {
            val distance = abs(newY - pos.centerY)
            if (distance < closestDistance) {
                newIndex = index
                closestDistance = distance
            }
        }
        return newIndex
    }

    private fun repositionOnDrop() {
        for (i in circles.indices) {
            val circle = circles[i]
            val label = labels[i]
            circle.setLocation(circlesPositions[i].x, circlesPositions[i].y)
            label.setLocation(label.x, labelsPositions[i].y)
        }
    }

    private fun repositionOnDrag() {
        for ((i, other) in circles.withIndex()) {
            if (circle != other) {
                val circle = circles[i]
                val label = labels[i]
                circle.setLocation(circlesPositions[i].x, circlesPositions[i].y)
                label.setLocation(label.x, labelsPositions[i].y)
            }
        }
    }

    private fun getLabel(
        circle: CirclePanel,
        labels: List<JBLabel>,
    ): JBLabel {
        for (l in labels) {
            if (l.labelFor == circle) {
                return l
            }
        }
        return JBLabel()
    }

    private fun indicateLimitedVerticalMovement(newCircleY: Int) {
        if (newCircleY <= minY || newCircleY >= maxY) {
            // If the circle reaches the upper or lower limit, adjust positions of all circles
            val deltaAllCircles =
                if (newCircleY <= minY) {
                    // If the circle reaches the upper limit, move all circles down
                    minY - newCircleY
                } else {
                    // If the circle reaches the lower limit, move all circles up
                    maxY - newCircleY
                }

            // Move all circles accordingly to maintain the visual indication
            moveAllCircles(deltaAllCircles)
        }
    }

    private fun moveAllCircles(delta: Int) {
        // Move all circles and labels
        for (i in circles.indices) {
            val circle = circles[i]
            val label = labels[i]

            val newCircleY = circle.y + delta
            circle.setLocation(circle.x, newCircleY)
            label.setLocation(label.x, newCircleY)
        }
    }
}

data class CirclePosition(
    val centerX: Int,
    val centerY: Int,
    val x: Int,
    val y: Int,
)
