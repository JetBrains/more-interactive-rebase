package com.jetbrains.interactiveRebase.listeners

import CirclePanel
import com.intellij.openapi.components.service
import com.jetbrains.interactiveRebase.services.ComponentService
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener

/**
 * A listener that allows a circle panel to be hovered on.
 * Involves the implementation of three methods for different type of
 * mouse actions that all reflect different parts of a "hover" action
 */
class CircleHoverListener(private val circlePanel: CirclePanel) : MouseListener, MouseMotionListener {
    private lateinit var componentService: ComponentService

    /**
     * Highlight the circle if the mouse enters the encapsulating rectangle and
     * is within the drawn circle.
     */

    override fun mouseEntered(e: MouseEvent?) {
        if (e != null && circlePanel.circle.contains(e.x.toDouble(), e.y.toDouble())) {
            circlePanel.commit.isHovered = true
            circlePanel.repaint()
        }
    }

    /**
     * Remove hovering if the mouse exits the encapsulating rectangle.
     */

    override fun mouseExited(e: MouseEvent?) {
        if (e != null && !circlePanel.circle.contains(e.x.toDouble(), e.y.toDouble())) {
            circlePanel.commit.isHovered = false
            circlePanel.repaint()
        }
    }

    /**
     * Select a commit upon a click.
     */
    override fun mouseClicked(e: MouseEvent?) {
        componentService = circlePanel.commit.project.service<ComponentService>()
        circlePanel.commit.isSelected = !circlePanel.commit.isSelected
        componentService.toggleCommitSelection(circlePanel.commit)
        circlePanel.repaint()
        println("Selected commits: ${componentService.getSelectedCommits()}")
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
        throw UnsupportedOperationException("mousePressed is not supported for the CircleHoverListener")
    }

    /**
     * mouseReleased is not yet implemented
     */
    override fun mouseReleased(e: MouseEvent?) {
        throw UnsupportedOperationException("mouseReleased is not supported for the CircleHoverListener")
    }

    /**
     * mouseDragged is not yet implemented
     */
    override fun mouseDragged(e: MouseEvent?) {
        throw UnsupportedOperationException("mouseDragged is not supported for the CircleHoverListener")
    }
}
