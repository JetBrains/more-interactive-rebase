package com.jetbrains.interactiveRebase.listeners

import CirclePanel
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener

/**
 * A listener that allows a circle panel to be hovered on.
 * Involves the implementation of three methods for different type of
 * mouse actions that all reflect different parts of a "hover" action
 */
class CircleHoverListener(private val circlePanel: CirclePanel) : MouseListener, MouseMotionListener {
    /**
     * Highlight the circle if the mouse enters the encapsulating rectangle and
     * is within the drawn circle.
     */
    override fun mouseEntered(e: MouseEvent?) {
        if (e != null && circlePanel.circle.contains(e.x.toDouble(), e.y.toDouble())) {
            circlePanel.isHovering = true
            circlePanel.repaint()
        }
    }

    /**
     * Remove hovering if the mouse exits the encapsulating rectangle.
     */
    override fun mouseExited(e: MouseEvent?) {
        if (e != null && !circlePanel.circle.contains(e.x.toDouble(), e.y.toDouble())) {
            circlePanel.isHovering = false
            circlePanel.repaint()
        }
    }

    /**
     * Select a commit upon a click.
     */
    override fun mouseClicked(e: MouseEvent?) {
        circlePanel.isSelected = !circlePanel.isSelected
        circlePanel.repaint()
    }

    /**
     * Highlights a circle if the mouse has entered the encapsulating rectangle panel
     * and has subsequently moved within the panel,
     * so that it is now within the circle boundaries.
     */
    override fun mouseMoved(e: MouseEvent?) {
        circlePanel.isHovering = e != null && circlePanel.circle.contains(e.x.toDouble(), e.y.toDouble())
        circlePanel.repaint()
    }

    override fun mousePressed(e: MouseEvent?) {
        TODO("Not yet implemented")
    }

    override fun mouseReleased(e: MouseEvent?) {
        TODO("Not yet implemented")
    }

    override fun mouseDragged(e: MouseEvent?) {
        TODO("Not yet implemented")
    }
}
