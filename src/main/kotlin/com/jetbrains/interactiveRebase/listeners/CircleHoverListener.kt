package com.jetbrains.interactiveRebase.listeners

import CirclePanel
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener

class CircleHoverListener(private val circlePanel: CirclePanel) : MouseListener, MouseMotionListener {
    override fun mouseEntered(e: MouseEvent?) {
        if (e != null && circlePanel.circle.contains(e.x.toDouble(), e.y.toDouble())) {
            circlePanel.isHovering = true
            circlePanel.repaint()
        }
    }

    override fun mouseExited(e: MouseEvent?) {
        if (e != null && !circlePanel.circle.contains(e.x.toDouble(), e.y.toDouble())) {
            circlePanel.isHovering = false
            circlePanel.repaint()
        }
    }

    override fun mouseClicked(e: MouseEvent?) {
        circlePanel.isSelected = !circlePanel.isSelected
        circlePanel.repaint()
    }

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
