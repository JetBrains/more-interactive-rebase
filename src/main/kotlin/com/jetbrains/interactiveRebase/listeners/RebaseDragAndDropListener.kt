package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.visuals.GraphPanel
import com.jetbrains.interactiveRebase.visuals.LabeledBranchPanel
import com.jetbrains.interactiveRebase.visuals.Palette
import com.jetbrains.interactiveRebase.visuals.multipleBranches.RoundedPanel
import java.awt.Cursor
import java.awt.GridBagLayout
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Timer

class RebaseDragAndDropListener(
    val project: Project,
    private val mainBranchNameLabel: RoundedPanel,
    private val addedBranchNameLabel: RoundedPanel,
    private val graphPanel: GraphPanel,
) : MouseAdapter(), Disposable {
    private val mainBranchPanel = mainBranchNameLabel.parent as LabeledBranchPanel
    private val addedBranchPanel = addedBranchNameLabel.parent as LabeledBranchPanel
    private val dragPanel = project.service<ActionService>().mainPanel.dragPanel
    private val gbcMain = (mainBranchPanel.layout as GridBagLayout).getConstraints(mainBranchNameLabel)
    private val gbcAdded = (addedBranchPanel.layout as GridBagLayout).getConstraints(addedBranchNameLabel)

    internal var initialPositionMain = Point()
    internal var initialPositionAdded = Point()
    private var mousePosition = Point()
    private var mainPlaceholderPanel = placeholderPanel(mainBranchPanel)
    private var addedPlaceholderPanel = placeholderPanel(addedBranchPanel)

    private val initialColor = addedBranchNameLabel.backgroundColor
    private var wasDragged: Boolean = false

    /**
     * On pressing on a name label of a branch
     * 1. the initial positions of the name labels of both branches are stored
     * 2. sets flag that the label has not been dragged yet
     * 3. puts appropriate formatting on the label
     */
    override fun mousePressed(e: MouseEvent) {
        updateMousePosition(e)
        initialPositionMain =
            Point(
                mainBranchNameLabel.x + mainBranchPanel.x,
                mainBranchNameLabel.y + mainBranchPanel.y,
            )
        initialPositionAdded =
            Point(
                addedBranchNameLabel.x + addedBranchPanel.x,
                addedBranchNameLabel.y + addedBranchPanel.y,
            )

        wasDragged = false
        formatDraggedLabelOnDrag()
    }

    /**
     * On starting to drag the branch name label:
     * 1. the main branch name label moves to a drag panel laying over the graph panel
     * 2. the branch name label of the second branch is also moved to the drag panel
     * 3. transparent panels with the same sizing as the original ones are added
     * in their positions in the graph panel in order to keep the same layout
     * when revalidating; i.e., the layout doesn't jiggle during drag and drop
     * On continuous dragging of the branch name label:
     * 1. set the cursor to be the default cursor
     * 2. highlight the branch name label to indicate it's being dragged
     * 3. update the position of the branch name label according to mouse position
     * 4. highlight the second branch name label if we drag on top of it to
     * indicate user can drop on top of it
     */
    override fun mouseDragged(e: MouseEvent) {
        if (!wasDragged) {
            addLabelsToDragPanel()

            // Create a transparent placeholder panels
            // at the initial positions of the name labels
            substituteLabelForPlaceholderMainBranch()

            substituteLabelForPlaceholderAddedBranch()
            wasDragged = true
        }

        dragPanel.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
        formatDraggedLabelOnDrag()
        setBranchNameLocation(e)
        updateMousePosition(e)

        indicateDraggedLabelCanBeDroppedOnTheSecondLabel()
        renderCurvedArrow()
    }

    /**
     * On drop of the branch name label:
     * 1. resets the formatting of the labels that was set on drag to highlight them
     * 2. checks if the branch name label was dropped on top of the other branch label
     * if true
     * 2.1. change formatting of the label to indicate branch was rebased
     * 2.2. does the actual rebase
     * 3. puts the labels from the drag panel back to the graph panel
     * 4. refreshes the drag panel (to visually remove the labels from it)
     */
    override fun mouseReleased(e: MouseEvent) {
        dragPanel.labelIsDragged = false
        resetFormattingOfSecondLabel()
        if (mainBranchPanel.branch.isRebased) {
            formatDraggedLabelOnDrop()
        } else {
            mainBranchNameLabel.backgroundColor = mainBranchPanel.colorTheme.branchNameColor
        }
        dragPanel.repaint()
        if (mainBranchNameLabel.bounds.intersects(addedBranchNameLabel.bounds) &&
            addedBranchPanel.branchPanel.circles.size != 1
        ) {
            formatDraggedLabelOnDrop()
            rebase()
        }

        returnNameLabelsBackInGraph()

        refreshDraggableArea()
    }

    /**
     * Draws a curved arrow to indicate
     * the target the label should be dragged towards
     */
    internal fun renderCurvedArrow() {
        dragPanel.labelIsDragged = true
        dragPanel.startDragPoint =
            Point(
                mainBranchNameLabel.x + mainBranchNameLabel.width / 2,
                mainBranchNameLabel.y + mainBranchNameLabel.height + 5,
            )
        dragPanel.endDragPoint =
            Point(
                addedBranchNameLabel.x + addedBranchNameLabel.width / 2,
                addedBranchNameLabel.y + addedBranchNameLabel.height + 5,
            )
        dragPanel.repaint()
    }

    /**
     * Changes the formatting of the second
     */
    internal fun indicateDraggedLabelCanBeDroppedOnTheSecondLabel() {
        if (mainBranchNameLabel.bounds.intersects(addedBranchNameLabel.bounds)) {
            changeFormattingOfSecondLabelWhenUserCanDropOnIt()
        } else {
            resetFormattingOfSecondLabel()
        }
    }

    /**
     * Make the second branch name label gray
     * to indicate you can drop on it
     */
    internal fun changeFormattingOfSecondLabelWhenUserCanDropOnIt() {
        addedBranchNameLabel.backgroundColor = JBColor.LIGHT_GRAY
        addedBranchNameLabel.repaint()
    }

    /**
     * Changes the formatting of the dragged label to indicate
     * that is being dragged
     */
    internal fun formatDraggedLabelOnDrag() {
        if (mainBranchPanel.branch.isRebased) {
            mainBranchNameLabel.addBackgroundGradient(
                mainBranchPanel.colorTheme.regularCircleColor,
                addedBranchPanel.colorTheme.regularCircleColor,
            )
            mainBranchNameLabel.repaint()
        } else {
            mainBranchNameLabel.backgroundColor = mainBranchPanel.colorTheme.regularCircleColor
        }
    }

    /**
     * Brings the labels back to the graph panel
     * from the dragging area
     */
    internal fun returnNameLabelsBackInGraph() {
        mainBranchPanel.remove(mainPlaceholderPanel)
        mainBranchPanel.add(mainBranchNameLabel, gbcMain)

        addedBranchPanel.remove(addedPlaceholderPanel)
        addedBranchPanel.add(addedBranchNameLabel, gbcAdded)
    }

    /**
     * Calculates the correct offset for the animation
     * and updates branch info to the rebased values
     */
    internal fun rebase() {
        val (initialOffsetMain, initialOffsetAdded) = graphPanel.computeVerticalOffsets()
        graphPanel.graphInfo.addedBranch!!.baseCommit =
            graphPanel.graphInfo.addedBranch!!.currentCommits[0]
        graphPanel.graphInfo.mainBranch.isRebased = true
        val (finalOffsetMain, finalOffsetAdded) = graphPanel.computeVerticalOffsets()

        animateAndPropagateToBackend(
            initialOffsetMain,
            initialOffsetAdded,
            finalOffsetMain,
            finalOffsetAdded,
        )
    }

    /**
     * Set the format of the second branch label to
     * be the initial formatting
     */
    internal fun resetFormattingOfSecondLabel() {
        addedBranchNameLabel.backgroundColor = initialColor
        addedBranchNameLabel.repaint()
    }

    /**
     * Change the formatting of the branch label name
     * to indicate the branch has been rebased
     */
    internal fun formatDraggedLabelOnDrop() {
        mainBranchNameLabel.addBackgroundGradient(
            mainBranchPanel.colorTheme.branchNameColor,
            addedBranchPanel.colorTheme.branchNameColor,
        )

        mainBranchNameLabel.backgroundColor = Palette.TRANSPARENT
        mainBranchNameLabel.repaint()
    }

    /**
     * Make the placeholder panel
     * for the second branch name label
     * and add it to the position of the second branch name label
     */
    internal fun substituteLabelForPlaceholderAddedBranch() {
//        addedBranchPanel.remove(addedBranchNameLabel)
        addedBranchPanel.add(addedPlaceholderPanel, gbcAdded)
        addedBranchPanel.revalidate()
        addedBranchPanel.repaint()
    }

    /**
     * Make the placeholder panel
     * for the first branch name label
     * and add it to the position of the first branch name label
     */
    internal fun substituteLabelForPlaceholderMainBranch() {
        mainBranchPanel.add(mainPlaceholderPanel, gbcMain)
        mainBranchPanel.revalidate()
        mainBranchPanel.repaint()
    }

    /**
     * Puts the branch name labels
     * in the dragging area and
     * sets their positions to the initial ones
     */
    internal fun addLabelsToDragPanel() {
        dragPanel.add(mainBranchNameLabel)
        dragPanel.add(addedBranchNameLabel)
        mainBranchNameLabel.location = initialPositionMain
        addedBranchNameLabel.location = initialPositionAdded
        refreshDraggableArea()
    }

    /**
     * Refreshes the dragging area
     */
    internal fun refreshDraggableArea() {
        dragPanel.revalidate()
        dragPanel.repaint()
    }

    /**
     * Instantiates a placeholder panel
     */
    internal fun placeholderPanel(labeledBranchPanel: LabeledBranchPanel): RoundedPanel {
        val placeholderPanel = labeledBranchPanel.instantiateBranchNamePanel()
        placeholderPanel.backgroundColor = Palette.TRANSPARENT
        placeholderPanel.getComponent(0).foreground = labeledBranchPanel.background
        placeholderPanel.isOpaque = false
        placeholderPanel.removeBorderGradient()
        placeholderPanel.removeBackgroundGradient()
        return placeholderPanel
    }

    /**
     * Sets the location of the dragged label
     * and puts constraints on it so one
     * cannot drag it outside the content panel
     */
    internal fun setBranchNameLocation(e: MouseEvent) {
        val thisX = mainBranchNameLabel.x
        val thisY = mainBranchNameLabel.y

        val deltaX = e.xOnScreen - mousePosition.x
        val deltaY = e.yOnScreen - mousePosition.y
        val newX = thisX + deltaX
        val newY = thisY + deltaY

        val newXBounded = (newX).coerceIn(0, dragPanel.width - mainBranchNameLabel.width)
        val newYBounded = (newY).coerceIn(0, dragPanel.height - mainBranchNameLabel.height)
        mainBranchNameLabel.setLocation(newXBounded, newYBounded)
    }

    /**
     * Executes the animation of rebasing
     * and adds the actual rebase command in
     * the backend
     */
    internal fun animateAndPropagateToBackend(
        initialOffsetMain: Int,
        initialOffsetAdded: Int,
        finalOffsetMain: Int,
        finalOffsetAdded: Int,
        duration: Int = 300,
        delay: Int = 10,
    ) {
        val steps = duration / delay
        val incrementMain = ((finalOffsetMain - initialOffsetMain).toDouble() / steps.toDouble()).toInt()
        val incrementAdded = ((finalOffsetAdded - initialOffsetAdded).toDouble() / steps.toDouble()).toInt()
        var currentOffsetMain = initialOffsetMain
        var currentOffsetAdded = initialOffsetAdded
        val timer =
            Timer(delay) {
                if (currentOffsetMain > finalOffsetMain || currentOffsetAdded < finalOffsetAdded) {
                    currentOffsetMain += incrementMain
                    updateOffsetOfMainBranch(currentOffsetMain)
                    currentOffsetAdded += incrementAdded
                    updateOffsetOfAddedBranch(currentOffsetAdded)
                } else {
                    (it.source as Timer).stop()
                    updateOffsetOfAddedBranch(finalOffsetAdded)
                    updateOffsetOfMainBranch(finalOffsetMain)
                    if (mainBranchPanel.branch.isRebased) {
                        project.service<ActionService>().takeNormalRebaseAction()
                        project.service<ActionService>().mainPanel.revalidate()
                        project.service<ActionService>().mainPanel.repaint()
                    }
                }
            }
        timer.initialDelay = 0
        timer.isRepeats = true
        timer.start()
    }

    /**
     * Updates the position of the added branch
     * during animation
     */
    internal fun updateOffsetOfAddedBranch(offset: Int) {
        addedBranchPanel.addBranchWithVerticalOffset(offset)
        graphPanel.repaint()
        addedBranchPanel.revalidate()
        addedBranchPanel.repaint()
    }

    /**
     * Updates the position of the main branch
     * during animation
     */
    internal fun updateOffsetOfMainBranch(offset: Int) {
        mainBranchPanel.addBranchWithVerticalOffset(offset)
        graphPanel.repaint()
        mainBranchPanel.revalidate()
        mainBranchPanel.repaint()
    }

    /**
     * Updates the coordinates of the mouse cursor
     * upon mouse press and while dragging
     */
    internal fun updateMousePosition(e: MouseEvent) {
        mousePosition.x = e.xOnScreen
        mousePosition.y = e.yOnScreen
    }

    override fun dispose() {
    }
}
