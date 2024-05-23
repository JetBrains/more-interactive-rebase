package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.Disposable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.visuals.CirclePanel
import com.jetbrains.interactiveRebase.visuals.LabeledBranchPanel
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Listener that handles drag and drop actions
 * of single commits within a branch
 */
class CircleDragAndDropListener(
    private val circle: CirclePanel,
    private val circles: MutableList<CirclePanel>,
    private val parent: LabeledBranchPanel,
) : MouseAdapter(), Disposable {
    private var messages: MutableList<JBPanel<JBPanel<*>>> = parent.messages
    private var labels = parent.commitLabels
    private var label = labels[getLabelIndex(circle, labels)]
    private var message: JBPanel<JBPanel<*>> = messages[getLabelIndex(circle, labels)]
    private var currentPosition = Point(circle.x, circle.y)
    private var circlesPositions = mutableListOf<CirclePosition>()
    private var messagesPositions = mutableListOf<Point>()

    /**
     * Indicators for the vertical limitations of circle movements,
     * so that a commit doesn't get outside the parent panel
     */
    private val minY = 0
    private var maxY = 200

    /**
     * Necessary to differentiate between mouse release
     * for dropping and
     * mouse click for selecting.
     */
    private var wasDragged = false

    /**
     * NEW: UPDATES CommitInfo
     */
    private val commit = circle.commit
    private val commits = parent.branch.currentCommits
    private var currentIndex = commits.indexOf(commit)
    private val initialIndex = commits.indexOf(commit)

    init {
        SwingUtilities.invokeLater {
            maxY = parent.branchPanel.height - circle.height
        }
    }

    /**
     * On mouse press, do the following for a branch:
     * 1. set current position of the commit
     * 2. find positions of the circles
     * 3. find positions of the labels
     */
    override fun mousePressed(e: MouseEvent) {
        wasDragged = false
        currentPosition.x = e.xOnScreen
        currentPosition.y = e.yOnScreen
        circlesPositions =
            circles.map { c ->
                CirclePosition(c.centerX.toInt(), c.centerY.toInt(), c.x, c.y)
            }.toMutableList()
        messagesPositions =
            messages.map { m ->
                Point(m.x, m.y)
            }.toMutableList()
    }

    /**
     * On mouse drag:
     * 1. visual indication through formatting
     * 2. update circle location to follow the mouse movement dynamically
     * 3. update view dynamically and reposition all commits
     * 4. handle limited vertical movement (outside parent component)
     */
    override fun mouseDragged(e: MouseEvent) {
        wasDragged = true
        commit.setDraggedTo(true)
        val deltaY = e.yOnScreen - currentPosition.y
        val newCircleY = circle.y + deltaY

        setCurrentCircleLocation(newCircleY)

        currentPosition.x = e.xOnScreen
        currentPosition.y = e.yOnScreen

        val newIndex = findNewBranchIndex()
        if (newIndex != currentIndex) {
            updateIndices(newIndex, currentIndex)
            repositionOnDrag()
            parent.repaint()
            currentIndex = newIndex
        }

        // Handle visual indication of movement limits
        indicateLimitedVerticalMovement(newCircleY)
    }

    /**
     * On mouse release:
     * 1. drop and reposition commit if it was dragged;
     * 2. update main view (refresh)
     * 3. update commitInfo
     */
    override fun mouseReleased(e: MouseEvent) {
        if (wasDragged) {
            commit.setDraggedTo(false)
            repositionOnDrop()
            if (initialIndex != currentIndex) {
                commit.setReorderedTo(true)
            }
            parent.branch.updateCurrentCommits(initialIndex, currentIndex, commit)
        }
    }

    /**
     * Sets the current location of the circle
     * within the parent component
     */
    private fun setCurrentCircleLocation(newCircleY: Int) {
        // Check if the new position exceeds the upper or lower limit
        val newCircleYBounded = (newCircleY).coerceIn(minY, maxY)

        // Update the circle and label positions
        circle.setLocation(circle.x, newCircleYBounded)
        message.setLocation(message.x, newCircleYBounded)
    }

    /**
     * Update the indices:
     * 1. commit info current commit order
     * 2. circle panels order
     * 3. commit name message label order
     * 4. update neighbors
     * (each commit has pointers to previous and next commit)
     */
    private fun updateIndices(
        newIndex: Int,
        oldIndex: Int,
    ) {
        circles.removeAt(oldIndex)
        circles.add(newIndex, circle)
        messages.removeAt(oldIndex)
        messages.add(newIndex, message)

        updateNeighbors(oldIndex)
        updateNeighbors(newIndex)
    }

    /**
     * Updates the neighboring commits
     * of a reordered commit.
     * Set neighbor to null if no neighbor.
     */
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

    /**
     * Determine the new position of a circle within a branch
     * based on its location while being dragged with the mouse
     */
    private fun findNewBranchIndex(): Int {
        var newIndex = -1
        var closestDistance = Int.MAX_VALUE
        val newY = circle.y

        for ((index, pos) in circlesPositions.withIndex()) {
            val distance = abs(newY - pos.centerY)

            // TODO figure out margins for rearrangement
//            if (distance > 0) {
//                distance += circle.height
//            } else {
//                distance = abs(distance) - circle.height
//            }
            if (distance < closestDistance) {
                newIndex = index
                closestDistance = distance
            }
        }
        return newIndex
    }

    /**
     * Reposition all circles to spread apart properly
     * when a circle is dropped
     */
    private fun repositionOnDrop() {
        for (i in circles.indices) {
            val circle = circles[i]
            val message = messages[i]
            circle.setLocation(circlesPositions[i].x, circlesPositions[i].y)
            message.setLocation(label.x, messagesPositions[i].y)
        }
    }

    /**
     * Reposition all circles to spread away from
     * the circle that is being dragged (snaps abruptly)
     */
//    private fun repositionOnDrag() {
//        for ((i, other) in circles.withIndex()) {
//            if (circle != other) {
//                val circle = circles[i]
//                val message = messages[i]
//                circle.setLocation(circlesPositions[i].x, circlesPositions[i].y)
//                message.setLocation(message.x, messagesPositions[i].y)
//            }
//        }
//    }

    /**
     * Reposition all circles to spread away from
     * the circle that is being dragged (smooth animated transition)
     */
    private fun repositionOnDrag() {
        val animationDuration = 30 // Duration in milliseconds
        val animationSteps = 10 // Number of animation steps

        // Calculate the target positions for each circle
        val startPositions = circles.map { Point(it.x, it.y) }
        val targetPositions = circlesPositions.map { Point(it.x, it.y) }
        val stepSizes = mutableListOf<Point>()

        // Calculate step sizes for each circle
        for (i in circles.indices) {
            val startX = startPositions[i].x
            val startY = startPositions[i].y
            val targetX = targetPositions[i].x
            val targetY = targetPositions[i].y

            val stepX = ceil((targetX - startX).toDouble() / animationSteps).toInt()
            val stepY = ceil((targetY - startY).toDouble() / animationSteps).toInt()
            stepSizes.add(Point(stepX, stepY))
        }

        var currentStep = 0

        // Timer to perform the animation
        val timer = Timer((animationDuration / animationSteps.toDouble()).roundToInt()) {
            currentStep++
            for ((i, other) in circles.withIndex()) {
                if(other != circle) {
                    val message = messages[i]

                    val currentX = other.x
                    val currentY = other.y
                    val stepX = stepSizes[i].x
                    val stepY = stepSizes[i].y

                    // Move the circle and message to the next step
                    val newX = if (Math.abs(targetPositions[i].x - currentX) < Math.abs(stepX)) targetPositions[i].x else currentX + stepX
                    val newY = if (Math.abs(targetPositions[i].y - currentY) < Math.abs(stepY)) targetPositions[i].y else currentY + stepY
                    other.setLocation(newX, newY)
                    message.setLocation(message.x, newY)
                }
            }

            // Repaint the parent to reflect the changes
            parent.repaint()

            // Stop the timer when the animation is complete
            if (currentStep >= animationSteps) {
                (it.source as Timer).stop()
            }
        }

        timer.initialDelay = 0
        timer.isRepeats = true
        timer.start()
    }

    /**
     * Retrieves the index of the corresponding label
     * of the circle that is being dragged
     */
    private fun getLabelIndex(
        circle: CirclePanel,
        labels: List<JBLabel>,
    ): Int {
        return labels.indexOfFirst { it.labelFor == circle }
    }

    /**
     * Indicates the user is trying to drag a circle
     * too far up or too far down
     * by moving the entire branch up or down
     * to show that no further movement is possible
     */
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

    /**
     * Moves all circles (commits) to
     * indicate that the user cannot move the commit further
     */
    private fun moveAllCircles(delta: Int) {
        // Move all circles and labels
        for (i in circles.indices) {
            val circle = circles[i]
            val message = messages[i]

            val newCircleY = circle.y + delta
            circle.setLocation(circle.x, newCircleY)
            message.setLocation(message.x, newCircleY)
        }
    }

    override fun dispose() {
    }
}

/**
 * Data class that stores detailed information
 * about a circle position within a branch
 */
data class CirclePosition(
    val centerX: Int,
    val centerY: Int,
    val x: Int,
    val y: Int,
)
