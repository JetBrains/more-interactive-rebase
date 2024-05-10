package com.jetbrains.interactiveRebase.listeners

import java.awt.event.MouseAdapter
import com.intellij.openapi.components.service
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.visuals.CirclePanel
import java.awt.event.MouseEvent

/**
 * A listener that allows a circle panel to be hovered on.
 * Involves the implementation of three methods for different type of
 * mouse actions that all reflect different parts of a "hover" action
 */
class CircleHoverListener(private val circlePanel: CirclePanel) : MouseAdapter() {

    /**
     * Highlight the circle if the mouse enters the encapsulating rectangle and
     * is within the drawn circle.
     */

    override fun mouseEntered(e: MouseEvent?) {
        if (e != null && circlePanel.circle.contains(e.x.toDouble(), e.y.toDouble())) {
            circlePanel.commit.setHoveredTo(true)
            circlePanel.repaint()
        }
    }

    /**
     * Remove hovering if the mouse exits the encapsulating rectangle.
     */

    override fun mouseExited(e: MouseEvent?) {
        if (e != null && !circlePanel.circle.contains(e.x.toDouble(), e.y.toDouble())) {
            circlePanel.commit.setHoveredTo(false)
            circlePanel.repaint()
        }
    }

    /**
     * Select a commit upon a click.
     */
    override fun mouseClicked(e: MouseEvent?) {
        val modelService = circlePanel.commit.project.service<ModelService>()
        circlePanel.commit.isSelected = !circlePanel.commit.isSelected
        modelService.addOrRemoveCommitSelection(circlePanel.commit)
    }

    /**
     * Highlights a circle if the mouse has entered the encapsulating rectangle panel
     * and has subsequently moved within the panel,
     * so that it is now within the circle boundaries.
     */

    override fun mouseMoved(e: MouseEvent?) {
        circlePanel.commit.isHovered = e != null && circlePanel.circle.contains(e.x.toDouble(), e.y.toDouble())
        circlePanel.repaint()
    }

    /**
     * mousePressed is not yet implemented
     */
    override fun mousePressed(e: MouseEvent?) {
    }

    /**
     * mouseReleased is not yet implemented
     */
    override fun mouseReleased(e: MouseEvent?) {
    }

    /**
     * mouseDragged is not yet implemented
     */
    override fun mouseDragged(e: MouseEvent?) {
    }
}
