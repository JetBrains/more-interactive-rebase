package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.util.minimumHeight
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.visuals.CirclePanel
import com.jetbrains.interactiveRebase.visuals.DragPanel
import com.jetbrains.interactiveRebase.visuals.GraphPanel
import com.jetbrains.interactiveRebase.visuals.LabeledBranchPanel
import java.awt.Cursor
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Timer

class CherryDragAndDropListener(
    project: Project,
    private val cherry: CirclePanel,
    private val addedBranchPanel: LabeledBranchPanel,
) : MouseAdapter(), Disposable {
    private lateinit var clone: CirclePanel
    private val dragPanel: DragPanel = project.service<ActionService>().mainPanel.dragPanel
    private val graphPanel: GraphPanel = project.service<ActionService>().mainPanel.graphPanel
    private val mainBranchPanel: LabeledBranchPanel = graphPanel.mainBranchPanel
    private var gbc: GridBagConstraints = (addedBranchPanel.branchPanel.layout as GridBagLayout).getConstraints(cherry)
    private var mainCircles = mainBranchPanel.branchPanel.circles

    private var initialPositionCherry = Point()
    private var circlesPositions = mutableListOf<CirclePosition>()
    private var mousePosition = Point()
    private val initialIndex = addedBranchPanel.branchPanel.circles.indexOf(cherry)
    private var mainIndex = -1
    private var wasHoveringOnMainBranch: Boolean = false
    private var animationInProgress: Boolean = false
    private var wasDragged: Boolean = false

    override fun mousePressed(e: MouseEvent) {
        updateMousePosition(e)
        initialPositionCherry =
            Point(
                addedBranchPanel.x + addedBranchPanel.branchPanel.x + cherry.x,
                addedBranchPanel.y + addedBranchPanel.branchPanel.y + cherry.y,
            )
        circlesPositions =
            mainCircles.map { c ->
                CirclePosition(
                    c.centerX.toInt(),
                    c.centerY.toInt(),
                    c.x,
                    c.y,
                )
            }.toMutableList()

        wasHoveringOnMainBranch = false
        wasDragged = false
    }

    override fun mouseDragged(e: MouseEvent) {
        if (!wasDragged) {
            clone = createClone()
            formatCherryOnDrag()
            addCherryToDragPanel()
            wasDragged = true
        }
        dragPanel.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
        setCherryLocation(e)
        updateMousePosition(e)

        if (onTopOfMainBranch()) {
            val newIndex = findCherryIndex()
            if ((mainIndex != newIndex || !wasHoveringOnMainBranch) && !animationInProgress) {
                mainIndex = newIndex
                val (initialConstraints, finalConstraints, lineOffsets) = getConstraintsForRepositioning()
                animateTransition(initialConstraints, finalConstraints, lineOffsets)
            }
            wasHoveringOnMainBranch = true
        } else {
            if (wasHoveringOnMainBranch) {
                val (initialConstraints, finalConstraints, lineOffsets) = getConstraintsForReset()
                animateTransition(initialConstraints, finalConstraints, lineOffsets)
            }
            wasHoveringOnMainBranch = false
        }
    }

    private fun onTopOfMainBranch(): Boolean {
        val extendedRect = mainBranchPanel.branchPanel.bounds
        extendedRect.height += mainBranchPanel.branchPanel.diameter * 2
        return cherry.bounds.intersects(extendedRect)
    }

    private fun reposition(
        finalConstraints: List<Pair<GridBagConstraints, GridBagConstraints>>,
        lineOffsets: Pair<Int, Int>,
    ) {
        finalConstraints.forEachIndexed { index, (gbcCircle, gbcMessage) ->
            mainBranchPanel.branchPanel.add(mainCircles[index], gbcCircle)
            mainBranchPanel.labelPanelWrapper.add(mainBranchPanel.messages[index], gbcMessage)
        }

        graphPanel.lineOffset = lineOffsets.second

        graphPanel.revalidate()
        graphPanel.repaint()
    }

    private fun getConstraintsForReset(): Triple<
        List<Pair<GridBagConstraints, GridBagConstraints>>,
        List<Pair<GridBagConstraints, GridBagConstraints>>,
        Pair<Int, Int>,
    > {
        val initialConstraints = mutableListOf<Pair<GridBagConstraints, GridBagConstraints>>()
        val finalConstraints = mutableListOf<Pair<GridBagConstraints, GridBagConstraints>>()

        mainCircles.forEachIndexed { index, circle ->
            val message = mainBranchPanel.messages[index]

            // Get initial constraints
            val gbcCircleInitial = (mainBranchPanel.branchPanel.layout as GridBagLayout).getConstraints(circle)
            val gbcMessageInitial = (mainBranchPanel.labelPanelWrapper.layout as GridBagLayout).getConstraints(message)
            initialConstraints.add(Pair(gbcCircleInitial, gbcMessageInitial))

            // Create final constraints based on the logic in the method
            val gbcCircleFinal =
                (mainBranchPanel.branchPanel.layout as GridBagLayout).getConstraints(circle).apply {
                    insets.bottom = 0
                    if (index == 0) {
                        insets.top = mainBranchPanel.branchPanel.diameter
                    }
                }
            val gbcMessageFinal =
                (mainBranchPanel.labelPanelWrapper.layout as GridBagLayout).getConstraints(message).apply {
                    insets.bottom = 0
                    if (index == 0) {
                        insets.top = mainBranchPanel.branchPanel.diameter
                    }
                }
            finalConstraints.add(Pair(gbcCircleFinal, gbcMessageFinal))
        }

        val lineOffset = Pair(graphPanel.lineOffset, mainBranchPanel.branchPanel.diameter * 2)
        return Triple(initialConstraints, finalConstraints, lineOffset)
    }

    internal fun animateTransition(
        initialConstraints: List<Pair<GridBagConstraints, GridBagConstraints>>,
        finalConstraints: List<Pair<GridBagConstraints, GridBagConstraints>>,
        lineOffsets: Pair<Int, Int>,
        animationSteps: Int = 20,
        animationDuration: Int = 80,
    ) {
        if (animationInProgress) {
            return
        }

        animationInProgress = true

        var currentStep = 0

        // Initialize Swing Timer
        val timer =
            Timer(animationDuration / animationSteps) {
                if (currentStep < animationSteps) {
                    val progress = (currentStep + 1).toFloat() / animationSteps.toFloat()

                    // Update the constraints for each component
                    updateComponentsWithInterpolation(initialConstraints, finalConstraints, progress)

                    // Interpolate lineOffset
                    graphPanel.lineOffset = interpolateValue(lineOffsets.first, lineOffsets.second, progress)

                    // Repaint the graphPanel
                    graphPanel.repaint()

                    currentStep++
                } else {
                    reposition(finalConstraints, lineOffsets)
                    animationInProgress = false
                    // Stop the timer when animation is complete
                    (it.source as Timer).stop()
                }
            }

        // Start the timer
        timer.initialDelay = 0
        timer.isRepeats = true
        timer.start()
    }

    private fun updateComponentsWithInterpolation(
        initialConstraints: List<Pair<GridBagConstraints, GridBagConstraints>>,
        finalConstraints: List<Pair<GridBagConstraints, GridBagConstraints>>,
        progress: Float,
    ) {
        // Update the constraints for each component
        mainBranchPanel.branchPanel.removeAll()
        mainBranchPanel.labelPanelWrapper.removeAll()

        finalConstraints.forEachIndexed { index, (finalCircle, finalMessage) ->
            val initialCircle = initialConstraints[index].first
            val initialMessage = initialConstraints[index].second

            // Interpolate Insets
            val interpolatedCircleInsets = interpolateInsets(initialCircle.insets, finalCircle.insets, progress)
            val interpolatedMessageInsets = interpolateInsets(initialMessage.insets, finalMessage.insets, progress)

            // Update Insets in GridBagConstraints
            initialCircle.insets = interpolatedCircleInsets
            initialMessage.insets = interpolatedMessageInsets

            // Add components back to the panels with updated GridBagConstraints
            mainBranchPanel.branchPanel.add(mainCircles[index], initialCircle)
            mainBranchPanel.labelPanelWrapper.add(mainBranchPanel.messages[index], initialMessage)
        }

        // Revalidate the panels
        mainBranchPanel.branchPanel.revalidate()
        mainBranchPanel.labelPanelWrapper.revalidate()
    }

    private fun interpolateInsets(
        start: Insets,
        end: Insets,
        progress: Float,
    ): Insets {
        val top = (start.top + (end.top - start.top) * progress).toInt()
        val left = (start.left + (end.left - start.left) * progress).toInt()
        val bottom = (start.bottom + (end.bottom - start.bottom) * progress).toInt()
        val right = (start.right + (end.right - start.right) * progress).toInt()
        return Insets(top, left, bottom, right)
    }

    private fun interpolateValue(
        start: Int,
        end: Int,
        progress: Float,
    ): Int {
        return (start + (end - start) * progress).toInt()
    }

    private fun getConstraintsForRepositioning(): Triple<
        List<Pair<GridBagConstraints, GridBagConstraints>>,
        List<Pair<GridBagConstraints, GridBagConstraints>>,
        Pair<Int, Int>,
    > {
        val initialConstraints = mutableListOf<Pair<GridBagConstraints, GridBagConstraints>>()
        val finalConstraints = mutableListOf<Pair<GridBagConstraints, GridBagConstraints>>()

        for (i in mainCircles.indices) {
            val circle = mainCircles[i]
            val message = mainBranchPanel.messages[i]

            // Get initial constraints
            val gbcCircleInitial = (mainBranchPanel.branchPanel.layout as GridBagLayout).getConstraints(circle)
            val gbcMessageInitial = (mainBranchPanel.labelPanelWrapper.layout as GridBagLayout).getConstraints(message)
            initialConstraints.add(Pair(gbcCircleInitial, gbcMessageInitial))

            // Create final constraints based on the logic in the method
            val gbcCircleFinal =
                (mainBranchPanel.branchPanel.layout as GridBagLayout).getConstraints(circle).apply {
                    modifyInsetsToMakeSpaceForCherry(i)
                }
            val gbcMessageFinal =
                (mainBranchPanel.labelPanelWrapper.layout as GridBagLayout).getConstraints(message).apply {
                    modifyInsetsToMakeSpaceForCherry(i)
                }
            finalConstraints.add(Pair(gbcCircleFinal, gbcMessageFinal))
        }

        val finalLineOffset =
            if (mainCircles.size - 1 == mainIndex - 1) {
                mainBranchPanel.branchPanel.diameter * 2 + cherry.minimumHeight
            } else {
                mainBranchPanel.branchPanel.diameter * 2
            }

        val lineOffset = Pair(graphPanel.lineOffset, finalLineOffset)

        return Triple(initialConstraints, finalConstraints, lineOffset)
    }

    private fun GridBagConstraints.modifyInsetsToMakeSpaceForCherry(i: Int) {
        if (mainIndex == i + 1) {
            insets.bottom = cherry.minimumHeight
        } else {
            insets.bottom = 0
        }
        insets.top =
            if (i == 0 && mainIndex == 0) {
                mainBranchPanel.branchPanel.diameter + cherry.minimumHeight
            } else if (i == 0) {
                mainBranchPanel.branchPanel.diameter
            } else {
                0
            }
    }

    override fun mouseReleased(e: MouseEvent?) {
        if (wasDragged) {
            if (cherry.bounds.intersects(mainBranchPanel.branchPanel.bounds)) {
                mainIndex = findCherryIndex()
                mainBranchPanel.branchPanel.branch.currentCommits.add(mainIndex, createCherryCommit())
                addedBranchPanel.branchPanel.branch.currentCommits[initialIndex].isCherryPicked = true
                mainBranchPanel.updateCommits()
            } else {
                clone.commit.isCherryPicked = false
                cherry.commit.isCherryPicked = false
            }
            clone.commit.isHovered = false
            graphPanel.lineOffset = mainBranchPanel.branchPanel.diameter * 2
            graphPanel.updateGraphPanel()
            dragPanel.remove(cherry)
            dragPanel.remove(mainBranchPanel)
            refreshDraggableArea()
        }
        e?.consume()
    }

    private fun formatCherryOnDrag() {
        cherry.commit.isCherryPicked = true
    }

    private fun createCherryCommit(): CommitInfo {
        return CommitInfo(
            cherry.commit.commit,
            cherry.commit.project,
            cherry.commit.changes,
            cherry.commit.isSelected,
            cherry.commit.isHovered,
            cherry.commit.isTextFieldEnabled,
            cherry.commit.isSquashed,
            cherry.commit.isReordered,
            cherry.commit.isDragged,
            cherry.commit.isCollapsed,
            cherry.commit.isCherryPicked,
        )
    }

    /**
     * Sets the location of the dragged label
     * and puts constraints on it so one
     * cannot drag it outside the content panel
     */
    internal fun setCherryLocation(e: MouseEvent) {
        val thisX = cherry.x
        val thisY = cherry.y

        val deltaX = e.xOnScreen - mousePosition.x
        val deltaY = e.yOnScreen - mousePosition.y
        val newX = thisX + deltaX
        val newY = thisY + deltaY

        val newXBounded = (newX).coerceIn(0, dragPanel.width - cherry.width)
        val newYBounded = (newY).coerceIn(0, dragPanel.height - cherry.height)
        cherry.setLocation(newXBounded, newYBounded)
    }

    /**
     * Updates the coordinates of the mouse cursor
     * upon mouse press and while dragging
     */
    private fun updateMousePosition(e: MouseEvent) {
        mousePosition.x = e.xOnScreen
        mousePosition.y = e.yOnScreen
    }

    /**
     * Puts the cherry-picked commit
     * in the dragging area and
     * sets it positions to the initial one
     */
    internal fun addCherryToDragPanel() {
        addedBranchPanel.branchPanel.circles.add(initialIndex, clone)
        addedBranchPanel.branchPanel.add(clone, gbc)
        dragPanel.add(cherry)
        addedBranchPanel.branch.currentCommits[initialIndex] = clone.commit
        if (addedBranchPanel.branch.baseCommit == cherry.commit) {
            addedBranchPanel.branch.baseCommit = clone.commit
        }
        cherry.location = initialPositionCherry
        refreshDraggableArea()
        graphPanel.repaint()
    }

    /**
     * Refreshes the dragging area
     */
    private fun refreshDraggableArea() {
        dragPanel.revalidate()
        dragPanel.repaint()
    }

    private fun createClone(): CirclePanel {
        val clone = cherry.clone()
        clone.commit = createCherryCommit()
        clone.commit.isCherryPicked = false
        return clone
    }

    /**
     * Determine the new position of a circle within a branch
     * based on its location while being dragged with the mouse
     */
    internal fun findCherryIndex(): Int {
        var newIndex = 0
        val cherryY = cherry.y

        for ((index, pos) in circlesPositions.withIndex()) {
            if (cherryY > pos.y + mainBranchPanel.y + mainBranchPanel.branchPanel.y) {
                newIndex = index + 1
            }
        }
        return newIndex
    }

    override fun dispose() {
    }
}
