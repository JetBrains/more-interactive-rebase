package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.visuals.CirclePanel
import com.jetbrains.interactiveRebase.visuals.LabeledBranchPanel
import java.awt.Cursor
import java.awt.GridBagLayout
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

class CherryDragAndDropListener(
    project: Project,
    val cherry: CirclePanel,
    val addedBranchPanel: LabeledBranchPanel,
) : MouseAdapter(), Disposable {

    private val dragPanel = project.service<ActionService>().mainPanel.dragPanel
    private val graphPanel = project.service<ActionService>().mainPanel.graphPanel
    private val mainBranchPanel = graphPanel.mainBranchPanel
    private lateinit var clone: CirclePanel
    private val gbc = (addedBranchPanel.branchPanel.layout as GridBagLayout).getConstraints(cherry)
    private val mainCircles = mainBranchPanel.branchPanel.circles

    private var initialPositionCherry = Point()
    private var circlesPositions = mutableListOf<CirclePosition>()
    private var mousePosition = Point()
    private val initialIndex = addedBranchPanel.branchPanel.circles.indexOf(cherry)
    private var mainIndex = 0

    override fun mousePressed(e: MouseEvent) {
        clone = createClone()
        updateMousePosition(e)
        initialPositionCherry =
            Point(
                addedBranchPanel.x + addedBranchPanel.branchPanel.x + cherry.x,
                addedBranchPanel.y + addedBranchPanel.branchPanel.y + cherry.y,
            )
        circlesPositions =
            mainCircles.map { c ->
                CirclePosition(
                    mainBranchPanel.branchPanel.x + c.centerX.toInt(),
                    mainBranchPanel.branchPanel.y + c.centerY.toInt(),
                    mainBranchPanel.branchPanel.x + c.x,
                    mainBranchPanel.branchPanel.y + c.y
                )
            }.toMutableList()

        formatCherryOnPress()
        addCherryToDragPanel()
    }

    override fun mouseDragged(e: MouseEvent) {
        dragPanel.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
        formatCherryOnDrag()
        setCherryLocation(e)
        updateMousePosition(e)
    }

    override fun mouseReleased(e: MouseEvent?) {
        if (cherry.bounds.intersects(mainBranchPanel.branchPanel.bounds)) {
            mainIndex = findCherryIndex()
            mainBranchPanel.branchPanel.branch.currentCommits.add(mainIndex, createCherryCommit())
            mainBranchPanel.updateCommits()
        }
        graphPanel.updateGraphPanel()
        dragPanel.remove(cherry)
        refreshDraggableArea()
    }

    internal fun formatCherryOnDrag() {
        cherry.background = JBColor.GREEN
    }

    internal fun formatCherryOnPress() {
        cherry.commit.isCherryPicked = true
        cherry.repaint()
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
            cherry.commit.isCherryPicked
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
    internal fun updateMousePosition(e: MouseEvent) {
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
        dragPanel.add(cherry)
        addedBranchPanel.branchPanel.add(clone, gbc)
        cherry.location = initialPositionCherry
        addedBranchPanel.branchPanel.revalidate()
        addedBranchPanel.branchPanel.repaint()
        graphPanel.updateGraphPanel()
        refreshDraggableArea()
    }

    /**
     * Refreshes the dragging area
     */
    internal fun refreshDraggableArea() {
        dragPanel.revalidate()
        dragPanel.repaint()
    }

    internal fun createClone(): CirclePanel {
        val clone = cherry.clone()
        clone.commit = createCherryCommit()
        clone.commit.isCherryPicked = true
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
            if (cherryY > pos.y)
                newIndex = index + 1
        }
        return newIndex
    }

    override fun dispose() {
    }
}


