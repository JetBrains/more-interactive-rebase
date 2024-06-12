package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.visuals.GraphPanel
import com.jetbrains.interactiveRebase.visuals.multipleBranches.RoundedPanel
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

class RebaseDragAndDropListener(
    val project: Project,
    private val branchNamePanel: RoundedPanel,
    internal val otherBranchNamePanel: RoundedPanel,
    private val graphPanel: GraphPanel,
) : MouseAdapter(), Disposable {

    internal var initialPosition = Point(branchNamePanel.x, branchNamePanel.y)
    internal var mousePosition = Point(branchNamePanel.x, branchNamePanel.y)


    override fun mousePressed(e: MouseEvent) {
        updateMousePosition(e)
        initialPosition = Point(branchNamePanel.x, branchNamePanel.y)
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
    }

    override fun mouseReleased(e: MouseEvent) {
        if (branchNamePanel.bounds.intersects(otherBranchNamePanel.bounds)) {
            otherBranchNamePanel.background = JBColor.GREEN
        } else {
            branchNamePanel.location = initialPosition
        }
        otherBranchNamePanel.background = JBColor.WHITE
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