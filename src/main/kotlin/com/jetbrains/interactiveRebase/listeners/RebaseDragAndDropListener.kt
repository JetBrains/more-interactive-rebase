package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.visuals.GraphPanel
import com.jetbrains.interactiveRebase.visuals.LabeledBranchPanel
import com.jetbrains.interactiveRebase.visuals.Palette
import com.jetbrains.interactiveRebase.visuals.multipleBranches.RoundedPanel
import java.awt.GridBagLayout
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Timer

class RebaseDragAndDropListener(
    val project: Project,
    private val mainBranchNameLabel: RoundedPanel,
    internal val addedBranchNameLabel: RoundedPanel,
    private val graphPanel: GraphPanel,
) : MouseAdapter(), Disposable {

    private val mainBranchPanel = mainBranchNameLabel.parent as LabeledBranchPanel
    private val addedBranchPanel = addedBranchNameLabel.parent as LabeledBranchPanel
    private val dragPanel = graphPanel.parent.getComponent(0) as JBPanel<JBPanel<*>>
    private val gbcMain = (mainBranchPanel.layout as GridBagLayout).getConstraints(mainBranchNameLabel)
    private val gbcAdded = (addedBranchPanel.layout as GridBagLayout).getConstraints(addedBranchNameLabel)

    internal var initialPositionMain = Point()
    internal var initialPositionAdded = Point()
    internal var mousePosition = Point()
    private var mainPlaceholderPanel = placeholderPanel(mainBranchPanel)
    private var addedPlaceholderPanel = placeholderPanel(addedBranchPanel)

    private val initialColor = addedBranchNameLabel.backgroundColor

    override fun mousePressed(e: MouseEvent) {
        updateMousePosition(e)
        initialPositionMain = Point(
            mainBranchNameLabel.x + mainBranchPanel.x,
            mainBranchNameLabel.y + mainBranchPanel.y)
        initialPositionAdded = Point(
            addedBranchNameLabel.x + addedBranchPanel.x,
            addedBranchNameLabel.y + addedBranchPanel.y)

        addLabelsToDragPanel()

        // Create a transparent placeholder panel at the initial position
        substituteLabelForPlaceholderMainBranch()

        substituteLabelForPlaceholderAddedBranch()
    }

    private fun substituteLabelForPlaceholderAddedBranch() {
        addedBranchPanel.remove(addedBranchNameLabel)
        addedBranchPanel.add(addedPlaceholderPanel, gbcAdded)
        addedBranchPanel.revalidate()
        addedBranchPanel.repaint()
    }

    private fun substituteLabelForPlaceholderMainBranch() {
        mainBranchPanel.remove(mainBranchNameLabel)
        mainBranchPanel.add(mainPlaceholderPanel, gbcMain)
        mainBranchPanel.revalidate()
        mainBranchPanel.repaint()
    }

    private fun addLabelsToDragPanel() {
        dragPanel.add(mainBranchNameLabel)
        dragPanel.add(addedBranchNameLabel)
        mainBranchNameLabel.location = initialPositionMain
        addedBranchNameLabel.location = initialPositionAdded
        dragPanel.revalidate()
        dragPanel.repaint()
    }

    private fun placeholderPanel(labeledBranchPanel: LabeledBranchPanel): RoundedPanel {
        val placeholderPanel = labeledBranchPanel.instantiateBranchNamePanel()
        placeholderPanel.backgroundColor = Palette.TRANSPARENT
        placeholderPanel.getComponent(0).foreground = labeledBranchPanel.background
        placeholderPanel.isOpaque = false
        return placeholderPanel
    }

    override fun mouseDragged(e: MouseEvent) {
        mainBranchNameLabel.backgroundColor = mainBranchPanel.colorTheme.regularCircleColor
        setBranchNameLocation(e)
        updateMousePosition(e)

        if (mainBranchNameLabel.bounds.intersects(addedBranchNameLabel.bounds)) {
            addedBranchNameLabel.backgroundColor = JBColor.LIGHT_GRAY
            addedBranchNameLabel.repaint()
        } else {
            addedBranchNameLabel.backgroundColor = initialColor
            addedBranchNameLabel.repaint()
        }
    }

    private fun setBranchNameLocation(e: MouseEvent) {
        val thisX = mainBranchNameLabel.x
        val thisY = mainBranchNameLabel.y

        val deltaX = e.xOnScreen - mousePosition.x
        val deltaY = e.yOnScreen - mousePosition.y
        val newX = thisX + deltaX
        val newY = thisY + deltaY

//        val newXBounded = (newX).coerceIn(0, dragPanel.width - mainBranchNameLabel.width)
//        val newYBounded = (newY).coerceIn(0, initialPositionMain.y + mainBranchNameLabel.height + 30)
        mainBranchNameLabel.setLocation(newX, newY)
    }

    override fun mouseReleased(e: MouseEvent) {
        if (mainBranchNameLabel.bounds.intersects(addedBranchNameLabel.bounds)) {
            addedBranchNameLabel.backgroundColor = JBColor.GREEN
            addedBranchNameLabel.repaint()
//            graphPanel.graphInfo.addedBranch!!.rebaseOffset = 100
            graphPanel.graphInfo.addedBranch!!.baseCommit =
                graphPanel.graphInfo.addedBranch!!.currentCommits[0]
            project.service<ActionService>().takeNormalRebaseAction()
//            addedBranchPanel.revalidate()
//            animatePanelMovement(200)
        }

        mainBranchPanel.remove(mainPlaceholderPanel)
        mainBranchPanel.add(mainBranchNameLabel, gbcMain)

        addedBranchPanel.remove(addedPlaceholderPanel)
        addedBranchPanel.add(addedBranchNameLabel, gbcAdded)

        graphPanel.revalidate()
        graphPanel.updateGraphPanel()
        graphPanel.repaint()
        dragPanel.revalidate()
        dragPanel.repaint()
    }

    fun animatePanelMovement(offset: Int) {
        val startY = addedBranchPanel.y
        val targetY = startY + offset
        val animationDuration = 500 // duration in milliseconds
        val animationInterval = 10 // interval in milliseconds
        val steps = animationDuration / animationInterval
        val stepSize = offset / steps.toFloat()

        val timer = Timer(animationInterval) { actionEvent ->
            val currentY = addedBranchPanel.y
            if (currentY < targetY) {
                addedBranchPanel.setLocation(addedBranchPanel.x, (currentY + stepSize).toInt())
                addedBranchPanel.revalidate()
                addedBranchPanel.repaint()
            } else {
                (actionEvent.source as Timer).stop()
            }
        }
        timer.start()
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