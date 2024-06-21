package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.util.minimumHeight
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.services.RebaseInvoker
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
    val project: Project,
    private val cherry: CirclePanel,
    private val addedBranchPanel: LabeledBranchPanel,
) : MouseAdapter(), Disposable {
    private lateinit var clone: CirclePanel
    private val dragPanel: DragPanel = project.service<ActionService>().mainPanel.dragPanel
    private lateinit var graphPanel: GraphPanel
    private lateinit var mainBranchPanel: LabeledBranchPanel
    private var gbc: GridBagConstraints =
        (addedBranchPanel.branchPanel.layout as GridBagLayout).getConstraints(cherry)
    private lateinit var gbcMainBranch: GridBagConstraints
    private lateinit var gbcAddedBranch: GridBagConstraints
    private lateinit var mainCircles: MutableList<CirclePanel>

    private var initialPositionCherry = Point()
    private var defaultLineOffset: Int = 30
    private var circlesPositions = mutableListOf<CirclePosition>()
    private var mousePosition = Point()
    private val initialIndex = addedBranchPanel.branchPanel.circles.indexOf(cherry)
    private var mainIndex = -1
    private var wasHoveringOnMainBranch: Boolean = false
    private var animationInProgress: Boolean = false
    private var wasDragged: Boolean = false

    override fun mousePressed(e: MouseEvent) {
        initializeLateFields()
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

    private fun initializeLateFields() {
        graphPanel = project.service<ActionService>().mainPanel.graphPanel
        mainBranchPanel = graphPanel.mainBranchPanel
        gbcMainBranch = (graphPanel.layout as GridBagLayout).getConstraints(mainBranchPanel)
        gbcAddedBranch = (graphPanel.layout as GridBagLayout).getConstraints(addedBranchPanel)
        mainCircles = mainBranchPanel.branchPanel.circles
        defaultLineOffset = graphPanel.lineOffset
    }

    override fun mouseDragged(e: MouseEvent) {
        if (!wasDragged) {
            setUpDrag()
        }
        dragPanel.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
        refreshDraggableArea()
        setCherryLocation(e)
        updateMousePosition(e)

        if (isOverMainBranch()) {
            makeSpaceForCherryOnMainBranch()
        } else {
            turnBackToTheInitialLayoutOfMainBranch()
        }
    }

    override fun mouseReleased(e: MouseEvent?) {
        if (wasDragged) {
            if (isOverMainBranch()) {
                propagateCherryPickToBackend()
            } else {
                resetCherryFormatting()
            }
            clone.commit.isHovered = false
            graphPanel.lineOffset = defaultLineOffset
            graphPanel.updateGraphPanel()
            addedBranchPanel.branchPanel.repaint()
            emptyDraggableArea()
            refreshDraggableArea()
        }
        e?.consume()
    }

    private fun emptyDraggableArea() {
        dragPanel.remove(cherry)
        dragPanel.remove(mainBranchPanel)
    }

    private fun resetCherryFormatting() {
        clone.commit.wasCherryPicked = false
        cherry.commit.wasCherryPicked = false
    }

    private fun propagateCherryPickToBackend() {
        project.service<RebaseInvoker>().undoneCommands.clear()
        project.service<ActionService>().prepareCherry(cherry.commit, mainIndex)
        addedBranchPanel.branchPanel.branch.currentCommits[initialIndex].wasCherryPicked = true

        project.service<ModelService>().graphInfo.addedBranch?.clearSelectedCommits()
        project.service<ModelService>().branchInfo.clearSelectedCommits()
        mainBranchPanel.branchPanel.updateCommits()
    }

    private fun turnBackToTheInitialLayoutOfMainBranch() {
        if (wasHoveringOnMainBranch) {
            val (initialConstraints, finalConstraints, lineOffsets) = getConstraintsForReset()
            animateTransition(initialConstraints, finalConstraints, lineOffsets)
        }
        wasHoveringOnMainBranch = false
    }

    private fun makeSpaceForCherryOnMainBranch() {
        val newIndex = findCherryIndex()
        if ((mainIndex != newIndex || !wasHoveringOnMainBranch) && !animationInProgress) {
            mainIndex = newIndex
            val (initialConstraints, finalConstraints, lineOffsets) = getConstraintsForRepositioning()
            animateTransition(initialConstraints, finalConstraints, lineOffsets)
        }
        wasHoveringOnMainBranch = true
    }

    private fun setUpDrag() {
        clone = createClone()
        formatCherryOnDrag()
        addCherryToDragPanel()
        wasDragged = true
    }

    private fun isOverMainBranch(): Boolean {
        val extendedRect = mainBranchPanel.branchPanel.bounds
        extendedRect.height += defaultLineOffset
        return cherry.bounds.intersects(extendedRect)
    }

//    private fun repositionToEndState(
//        finalConstraints: List<Pair<GridBagConstraints, GridBagConstraints>>,
//        lineOffsets: Pair<Int, Int>,
//    ) {
//        finalConstraints.forEachIndexed { index, (gbcCircle, gbcMessage) ->
//            mainBranchPanel.branchPanel.add(mainCircles[index], gbcCircle)
//            mainBranchPanel.labelPanelWrapper.add(mainBranchPanel.messages[index], gbcMessage)
//        }
//
//        graphPanel.lineOffset = lineOffsets.second
//
//        graphPanel.revalidate()
//        graphPanel.repaint()
//    }

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

            val gbcCircleFinal =
                (mainBranchPanel.branchPanel.layout as GridBagLayout).getConstraints(circle).apply {
                    resetInsetsToInitialMainBranchLayout(index)
                }
            val gbcMessageFinal =
                (mainBranchPanel.labelPanelWrapper.layout as GridBagLayout).getConstraints(message).apply {
                    resetInsetsToInitialMainBranchLayout(index)
                }
            finalConstraints.add(Pair(gbcCircleFinal, gbcMessageFinal))
        }

        val lineOffset = Pair(graphPanel.lineOffset, defaultLineOffset)
        return Triple(initialConstraints, finalConstraints, lineOffset)
    }

    private fun GridBagConstraints.resetInsetsToInitialMainBranchLayout(index: Int) {
        insets.bottom = 0
        if (index == 0) {
            insets.top = mainBranchPanel.branchPanel.diameter
        }
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

        val timer =
            Timer(animationDuration / animationSteps) {
                if (currentStep < animationSteps) {
                    currentStep++
                    val progress = (currentStep).toFloat() / animationSteps.toFloat()
//                    SwingUtilities.invokeLater {
                    updateComponentsWithInterpolation(initialConstraints, finalConstraints, progress)
//                    }
                    graphPanel.lineOffset = interpolateValue(lineOffsets.first, lineOffsets.second, progress)
//                    graphPanel.repaint()
                } else {
                    animationInProgress = false
                    (it.source as Timer).stop()
                }
            }

        // Start the timer
        startAnimation(timer)
    }

    private fun startAnimation(timer: Timer) {
        timer.initialDelay = 0
        timer.isRepeats = true
        timer.start()
    }

    private fun updateComponentsWithInterpolation(
        initialConstraints: List<Pair<GridBagConstraints, GridBagConstraints>>,
        finalConstraints: List<Pair<GridBagConstraints, GridBagConstraints>>,
        progress: Float,
    ) {
        finalConstraints.forEachIndexed { index, (finalCircle, finalMessage) ->
            val initialCircleGbc = initialConstraints[index].first
            val initialMessageGbc = initialConstraints[index].second

            // Interpolate Insets
            val interpolatedCircleInsets =
                interpolateInsets(
                    initialCircleGbc.insets,
                    finalCircle.insets,
                    progress,
                )
            val interpolatedMessageInsets =
                interpolateInsets(
                    initialMessageGbc.insets,
                    finalMessage.insets,
                    progress,
                )

            // Update Insets in GridBagConstraints
            initialCircleGbc.insets = interpolatedCircleInsets
            initialMessageGbc.insets = interpolatedMessageInsets

            // Add components back to the panels with updated GridBagConstraints
            mainBranchPanel.branchPanel
                .add(mainCircles[index], initialCircleGbc)
            mainBranchPanel.labelPanelWrapper
                .add(mainBranchPanel.messages[index], initialMessageGbc)
        }

        mainBranchPanel.branchPanel.revalidate()
        mainBranchPanel.branchPanel.repaint()
        mainBranchPanel.labelPanelWrapper.revalidate()
        mainBranchPanel.labelPanelWrapper.repaint()
        refreshDraggableArea()
    }

    private fun interpolateInsets(
        start: Insets,
        end: Insets,
        progress: Float,
    ): Insets {
        val top = interpolateValue(start.top, end.top, progress)
        val left = interpolateValue(start.left, end.left, progress)
        val bottom = interpolateValue(start.bottom, end.bottom, progress)
        val right = interpolateValue(start.right, end.right, progress)
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

        val finalLineOffset = computeFinalLineOffset()

        val lineOffset = Pair(graphPanel.lineOffset, finalLineOffset)

        return Triple(initialConstraints, finalConstraints, lineOffset)
    }

    private fun computeFinalLineOffset() =
        if (mainCircles.size - 1 == mainIndex - 1) {
            defaultLineOffset + cherry.minimumHeight
        } else {
            defaultLineOffset
        }

    private fun GridBagConstraints.modifyInsetsToMakeSpaceForCherry(i: Int) {
        if (mainIndex == i + 1) {
            insets.bottom = cherry.minimumHeight
//            if (i == mainCircles.size - 1) {
//                gbc.insets.bottom =
//                    mainBranchPanel.branchPanel.diameter + cherry.minimumHeight
//            }
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

    private fun formatCherryOnDrag() {
        cherry.commit.wasCherryPicked = true
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
            cherry.commit.wasCherryPicked,
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
        val placeholder = placeholderMainBranch()
        gbcMainBranch.insets.left = mainBranchPanel.branchPanel.diameter
        graphPanel.add(placeholder, gbcMainBranch)
        graphPanel.add(addedBranchPanel, gbcAddedBranch)
        dragPanel.add(mainBranchPanel)
        mainBranchPanel.setBounds(
            0,
            0,
            mainBranchPanel.width,
            mainBranchPanel.height +
                mainBranchPanel.branchPanel.diameter,
        )
        refreshDraggableArea()
        graphPanel.repaint()
    }

    private fun placeholderMainBranch(): JBPanel<JBPanel<*>> {
        val placeholder = JBPanel<JBPanel<*>>()
        placeholder.minimumSize = mainBranchPanel.minimumSize
        placeholder.preferredSize = mainBranchPanel.preferredSize
        placeholder.maximumSize = mainBranchPanel.maximumSize
        placeholder.isOpaque = false
        return placeholder
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
        // clone.commit = createCherryCommit()
        clone.commit.wasCherryPicked = false
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
