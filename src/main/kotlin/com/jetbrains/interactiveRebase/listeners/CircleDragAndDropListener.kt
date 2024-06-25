package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.dataClasses.commands.CollapseCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.ReorderCommand
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import com.jetbrains.interactiveRebase.visuals.CirclePanel
import com.jetbrains.interactiveRebase.visuals.GraphPanel
import com.jetbrains.interactiveRebase.visuals.LabeledBranchPanel
import com.jetbrains.interactiveRebase.visuals.Palette
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.ActionEvent
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
    val project: Project,
    private val circle: CirclePanel,
    internal val circles: MutableList<CirclePanel>,
    private val parent: LabeledBranchPanel,
) : MouseAdapter(), Disposable {
    internal var messages: MutableList<JBPanel<JBPanel<*>>> = parent.messages
    internal var labels = parent.commitLabels
    internal var label = labels[getLabelIndex(circle, labels)]
    internal var message: JBPanel<JBPanel<*>> = messages[getLabelIndex(circle, labels)]
    internal var circlesPositions = mutableListOf<CirclePosition>()
    internal var circlesThemes = mutableListOf<Palette.Theme>()
    internal var messagesPositions = mutableListOf<Point>()

    /**
     * Indicators for the vertical limitations of circle movements,
     * so that a commit doesn't get outside the parent panel
     */
    internal val minY = 0
    internal var maxY = 200

    /**
     * Necessary to differentiate between mouse release
     * for dropping and
     * mouse click for selecting.
     */
    internal var wasDragged = false

    /**
     * NEW: UPDATES CommitInfo
     */
    internal val commit = circle.commit
    internal val commits = parent.branch.currentCommits
    internal var currentIndex = commits.indexOf(commit)
    internal val initialIndex = commits.indexOf(commit)
    internal var squashIntoIndex: Int = -1

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
        squashIntoIndex = -1
        wasDragged = false
        circlesPositions =
            circles.map { c ->
                CirclePosition(c.centerX.toInt(), c.centerY.toInt(), c.x, c.y)
            }.toMutableList()

        circlesThemes =
            circles.map { c ->
                c.colorTheme
            }.toMutableList()
        messagesPositions =
            messages.map { m ->
                Point(m.x, m.y)
            }.toMutableList()
        e.consume()
    }

    override fun mouseDragged(e: MouseEvent) {
        maxY = parent.branchPanel.height - circle.height
        if (!commit.getChangesAfterPick().any { it is DropCommand || it is CollapseCommand } &&
            parent.branch.isWritable
        ) {
            parent.branchPanel.setComponentZOrder(circle, 0)
            wasDragged = true
            commit.isDragged = true
            val newCircleY = e.yOnScreen - parent.branchPanel.locationOnScreen.y - circle.height / 2

            setCurrentCircleLocation(newCircleY)

            val newIndex = findNewBranchIndex()
            circles.forEachIndexed { index, circle ->
                circle.colorTheme = circlesThemes[index]
                circle.commit.isHovered = false
                circle.repaint()
            }
            if (isHoveringOverCircle(newIndex - 1, newIndex + 1)) {
                val targetCircle = circles[squashIntoIndex]
                if (targetCircle.commit.getChangesAfterPick().any { it is DropCommand || it is CollapseCommand }) {
                    squashIntoIndex = -1
                } else {
                    circle.colorTheme = Palette.BLUE_THEME_LIGHT
                    circle.commit.isHovered = true
                    circle.repaint()
                    targetCircle.colorTheme = Palette.BLUE_THEME_LIGHT
                    targetCircle.commit.isHovered = true
                    targetCircle.repaint()
                }
            }
            if (newIndex != currentIndex) {
                updateIndices(newIndex, currentIndex)
                repositionOnDrag()
                parent.repaint()
                currentIndex = newIndex
            }

            // Handle visual indication of movement limits
//        indicateLimitedVerticalMovement(newCircleY)

            (parent.parent as GraphPanel).repaint()
        }
        e.consume()
    }

    /**
     * On mouse release:
     * 1. drop and reposition commit if it was dragged;
     * 2. update main view (refresh)
     * 3. update commitInfo
     */
    override fun mouseReleased(e: MouseEvent) {
        val modelService = project.service<ModelService>()
        if (wasDragged) {
            commit.isDragged = false
            circles.forEachIndexed { index, circle ->
                circle.colorTheme = circlesThemes[index]
                circle.commit.isHovered = false
                circle.repaint()
            }
            if (squashIntoIndex != -1) {
                modelService.addToSelectedCommits(circle.commit, parent.branch)
                modelService.addToSelectedCommits(circles[squashIntoIndex].commit, parent.branch)
                project.service<ActionService>().takeFixupAction()
            } else if (initialIndex != currentIndex) {
                repositionOnDrop()
                modelService.markCommitAsReordered(commit, initialIndex, currentIndex)
                parent.branch.updateCurrentCommits(initialIndex, currentIndex, commit)
            }
            (parent.parent as GraphPanel?)?.updateGraphPanel()
            (parent.parent as GraphPanel?)?.repaint()
        }
        e.consume()
    }

    /**
     * Marks a commit as a reordered by
     * 1. sets the isReordered flag to true
     * 2. adds a ReorderCommand
     * to the visual changes applied to the commit
     * 3. adds the Reorder Command to the Invoker
     * that holds an overview of all staged changes.
     */
    internal fun markCommitAsReordered() {
        commit.setReorderedTo(true)
        val command =
            ReorderCommand(
                commit,
                initialIndex,
                currentIndex,
            )
        commit.addChange(command)
        project.service<RebaseInvoker>().addCommand(command)
    }

    /**
     * Sets the current location of the circle
     * within the parent component
     */
    internal fun setCurrentCircleLocation(newCircleY: Int) {
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
    internal fun updateIndices(
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
    internal fun updateNeighbors(index: Int) {
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
    internal fun findNewBranchIndex(): Int {
        var newIndex = currentIndex
        val newY = circle.y + circle.height / 2

        for ((index, pos) in circlesPositions.withIndex()) {
            val targetCircleTop = pos.y + circle.height / 4
            val targetCircleBottom = pos.y + circle.height * 3 / 4

            if (newY < targetCircleTop && newIndex > index) {
                newIndex = index
                break
            }

            if (newY > targetCircleBottom && newIndex < index) {
                newIndex = index
                break
            }
        }
        return newIndex
    }

    private fun isHoveringOverCircle(
        indexForward: Int,
        indexBackward: Int,
    ): Boolean {
        // Check if indexForward is within bounds
        val forwardInBounds = indexForward >= 0 && indexForward < circles.size
        // Check if indexBackward is within bounds
        val backwardInBounds = indexBackward >= 0 && indexBackward < circles.size

        // Early return if both indices are out of bounds
        if (!forwardInBounds && !backwardInBounds) {
            squashIntoIndex = -1
            return false
        }

        // Initialize variables for forward bounds if in bounds
        val targetBoundsForwardTight =
            if (forwardInBounds) {
                val targetCircleForward = circles[indexForward]
                val targetBoundsForward = targetCircleForward.bounds
                Rectangle(
                    targetBoundsForward.x,
                    (targetBoundsForward.y + targetCircleForward.diameter * 0.5).toInt(),
                    targetBoundsForward.width,
                    (targetBoundsForward.height - targetCircleForward.diameter).toInt(),
                )
            } else {
                null
            }

        // Initialize variables for backward bounds if in bounds
        val targetBoundsBackwardTight =
            if (backwardInBounds) {
                val targetCircleBackward = circles[indexBackward]
                val targetBoundsBackward = targetCircleBackward.bounds
                Rectangle(
                    targetBoundsBackward.x,
                    (targetBoundsBackward.y + targetCircleBackward.diameter * 0.5).toInt(),
                    targetBoundsBackward.width,
                    (targetBoundsBackward.height - targetCircleBackward.diameter).toInt(),
                )
            } else {
                null
            }

        val draggingBounds = circle.bounds
        val draggingBoundsDecreased =
            Rectangle(
                draggingBounds.x,
                (draggingBounds.y + circle.diameter * 0.5).toInt(),
                draggingBounds.width,
                (draggingBounds.height - circle.diameter).toInt(),
            )

        // Check intersection with targetBoundsForwardTight if it exists
        if (targetBoundsForwardTight != null && draggingBoundsDecreased.intersects(targetBoundsForwardTight)) {
            squashIntoIndex = indexForward
            return true
        }

        // Check intersection with targetBoundsBackwardTight if it exists
        if (targetBoundsBackwardTight != null && draggingBoundsDecreased.intersects(targetBoundsBackwardTight)) {
            squashIntoIndex = indexBackward
            return true
        }

        // If neither intersection is true
        squashIntoIndex = -1
        return false
    }

    /**
     * Reposition all circles to spread apart properly
     * when a circle is dropped
     */
    internal fun repositionOnDrop() {
        for (i in circles.indices) {
            val circle = circles[i]
            val message = messages[i]
            circle.setLocation(circlesPositions[i].x, circlesPositions[i].y)
            message.setLocation(label.x, messagesPositions[i].y)
        }
    }

//    /**
//     * Reposition all circles to spread away from
//     * the circle that is being dragged (snaps abruptly)
//     */
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
    internal fun repositionOnDrag() {
        // Calculate the target positions for each circle
        val startPositions = circles.map { Point(it.x, it.y) }
        val targetPositions = circlesPositions.map { Point(it.x, it.y) }
        val stepSizes = calculateStepSizes(startPositions, targetPositions)

        val timer =
            createReorderAnimation(
                stepSizes,
                targetPositions,
            )

        startReorderAnimation(timer)
    }

    /**
     * Creates a transition that dynamically
     * rearranges all remaining circles in the branch
     * accordingly to the circle that is being dragged.
     * One can specify:
     * 1. the duration of the animation
     * 2. how many frames (steps) for smooth/jiggled execution
     * 3. custom step size for every circle
     * 4. target position for each circle
     */
    internal fun createReorderAnimation(
        stepSizes: MutableList<Point>,
        targetPositions: List<Point>,
        animationDuration: Int = 50,
        animationSteps: Int = 10,
    ): Timer {
        var currentStep = 0

        // Timer to perform the animation
        val timer =
            Timer((animationDuration / animationSteps.toDouble()).roundToInt()) {
                currentStep++
                moveCirclesAndMessages(stepSizes, targetPositions)
                stopAnimationIfComplete(currentStep, animationSteps, it)
            }
        return timer
    }

    /**
     * Starts playing the reorder animation.
     */
    internal fun startReorderAnimation(timer: Timer) {
        timer.initialDelay = 0
        timer.isRepeats = true
        timer.start()
    }

    /**
     *  Stop the timer
     *  when the animation is complete
     */
    internal fun stopAnimationIfComplete(
        currentStep: Int,
        animationSteps: Int,
        it: ActionEvent,
    ) {
        if (currentStep >= animationSteps) {
            (it.source as Timer).stop()
        }
    }

    /**
     * Iteratively moves all circles and messages
     * taking small steps
     * to their target positions
     */
    internal fun moveCirclesAndMessages(
        stepSizes: MutableList<Point>,
        targetPositions: List<Point>,
    ) {
        for ((i, other) in circles.withIndex()) {
            if (other != circle) {
                val message = messages[i]

                val currentX = other.x
                val currentY = other.y
                val stepX = stepSizes[i].x
                val stepY = stepSizes[i].y

                // Move the circle and message to the next step
                val newX =
                    reorderStep(targetPositions[i].x, currentX, stepX)
                val newY =
                    reorderStep(targetPositions[i].y, currentY, stepY)
                other.setLocation(newX, newY)
                message.setLocation(message.x, newY)
            }
        }
        parent.repaint()
    }

    /**
     * Calculates what is the new position
     * after a step is taken
     */
    internal fun reorderStep(
        target: Int,
        current: Int,
        step: Int,
    ) = if (abs(target - current) < abs(step)) target else current + step

    /**
     * Calculate the steps
     */
    internal fun calculateStepSizes(
        startPositions: List<Point>,
        targetPositions: List<Point>,
        animationSteps: Int = 10,
    ): MutableList<Point> {
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
        return stepSizes
    }

    /**
     * Retrieves the index of the corresponding label
     * of the circle that is being dragged
     */
    internal fun getLabelIndex(
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
    internal fun indicateLimitedVerticalMovement(newCircleY: Int) {
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
    internal fun moveAllCircles(delta: Int) {
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
