package com.jetbrains.interactiveRebase.visuals

import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import javax.swing.Icon

class RotatedIcon(val icon: Icon, val angle: Double): Icon {

    override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) {
        val g2d = g?.create() as? Graphics2D ?: return
        val iconWidth = icon.iconWidth
        val iconHeight = icon.iconHeight
        val centerX = x + iconWidth/2
        val centerY = y + iconHeight /2
        val originalTransform = g2d.transform

        g2d.rotate(Math.toRadians(angle), centerX.toDouble(), centerY.toDouble())

        icon.paintIcon(c, g2d, x, y)

        g2d.transform = originalTransform
    }

    override fun getIconWidth(): Int = icon.iconWidth
    override fun getIconHeight(): Int = icon.iconHeight
}