package com.jetbrains.interactiveRebase.visuals

import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import javax.swing.JButton

class RoundedButton(private val text: String = "", var backgroundColor: Color = Palette.TRANSPARENT, private val foreground: Color = Palette.TRANSPARENT) : JButton() {
    internal var arcWidth = 12
    internal var arcHeight = 12

    public override fun paintComponent(g: Graphics) {
        isContentAreaFilled = false
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.color = backgroundColor
        g2.fill(
            RoundRectangle2D.Double(
                1.0,
                3.0,
                width.toDouble() - 3.0,
                height.toDouble() - 5.0,
                arcWidth.toDouble(),
                arcHeight.toDouble(),
            ),
        )
        super.paintComponent(g2)
        g2.dispose()
        setText(text)
        setBackground(backgroundColor)
        setForeground(foreground)
    }
}
