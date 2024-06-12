package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.visuals.GraphPanel
import com.jetbrains.interactiveRebase.visuals.LabeledBranchPanel
import com.jetbrains.interactiveRebase.visuals.Palette
import com.jetbrains.interactiveRebase.visuals.multipleBranches.RoundedPanel
import java.awt.GridBagLayout
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLayeredPane

class RebaseDragAndDropListener(
    val project: Project,
    private val branchNamePanel: RoundedPanel,
    internal val otherBranchNamePanel: RoundedPanel,
    private val graphPanel: GraphPanel,
) : MouseAdapter(), Disposable {

//    private val layeredPane = graphPanel.layeredPane
    private val parent = branchNamePanel.parent as LabeledBranchPanel
    private val gbc = (parent.layout as GridBagLayout).getConstraints(branchNamePanel)

    internal var initialPosition = Point(branchNamePanel.x, branchNamePanel.y)
    internal var convertedPosition = Point(branchNamePanel.x, branchNamePanel.y)
    internal var mousePosition = Point(branchNamePanel.x, branchNamePanel.y)
    private var placeholderPanel = placeholderPanel()

    override fun mousePressed(e: MouseEvent) {

        updateMousePosition(e)
        initialPosition = Point(branchNamePanel.x, branchNamePanel.y)
        val offsetX = e.x - branchNamePanel.bounds.x
        val offsetY = e.y - branchNamePanel.bounds.y

        placeholderPanel = placeholderPanel()
        // Create a transparent placeholder panel at the initial position
        parent.remove(branchNamePanel)
        parent.add(placeholderPanel, gbc)
        parent.revalidate()
        parent.repaint()

        // Set the location of the panel to the mouse click point
//        layeredPane.add(branchNamePanel, JLayeredPane.DRAG_LAYER)
        graphPanel.add(branchNamePanel, JLayeredPane.DRAG_LAYER)
        branchNamePanel.setLocation(e.x - offsetX, e.y - offsetY)
    }

    private fun placeholderPanel(): RoundedPanel {
        val placeholderPanel = parent.instantiateBranchNamePanel()
        placeholderPanel.backgroundColor = Palette.TRANSPARENT
        placeholderPanel.getComponent(0).foreground = parent.background
        placeholderPanel.isOpaque = false
        return placeholderPanel
    }

    override fun mouseDragged(e: MouseEvent) {
        val thisX = branchNamePanel.x
        val thisY = branchNamePanel.y

        val deltaX = e.xOnScreen - mousePosition.x
        val deltaY = e.yOnScreen - mousePosition.y
        val newX = thisX + deltaX
        val newY = thisY + deltaY
        branchNamePanel.setLocation(newX, newY)
        updateMousePosition(e)

        if (branchNamePanel.bounds.intersects(otherBranchNamePanel.bounds)) {
            otherBranchNamePanel.background = JBColor.LIGHT_GRAY
        } else {
            otherBranchNamePanel.background = JBColor.BLACK
        }
        placeholderPanel.size = branchNamePanel.size
    }

    override fun mouseReleased(e: MouseEvent) {
        if (branchNamePanel.bounds.intersects(otherBranchNamePanel.bounds)) {
            otherBranchNamePanel.background = JBColor.GREEN
        } else {
            branchNamePanel.location = convertedPosition
        }
        otherBranchNamePanel.background = JBColor.WHITE
        parent.remove(placeholderPanel)
        parent.add(branchNamePanel, gbc)
//        layeredPane.revalidate()
//        layeredPane.repaint()
        graphPanel.revalidate()
        graphPanel.repaint()
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