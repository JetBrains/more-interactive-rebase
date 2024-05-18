package com.jetbrains.interactiveRebase.visuals.borders

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import org.jetbrains.plugins.notebooks.visualization.r.inlays.InlayDimensions.cornerRadius
import java.awt.*
import java.awt.geom.RoundRectangle2D
import javax.swing.border.AbstractBorder
import com.jetbrains.interactiveRebase.visuals.Palette
import java.awt.geom.Ellipse2D
import javax.swing.border.Border
class RoundedBorder(private val color : JBColor) : AbstractBorder() {
    override fun paintBorder(c: Component?, g: Graphics?, x: Int, y: Int, width: Int, height: Int) {
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g2.color = color
        val arc = 8

        g2.color = color
//        val arc = 30
        g2.stroke = BasicStroke(2f)
        g2.drawRoundRect(x, y, width, height, arc, arc)

//        g2.color = JBColor.MAGENTA
//        g2.fill(RoundRectangle2D.Double(x.toDouble(), y.toDouble(), width.toDouble(), height.toDouble(), arc.toDouble(), arc.toDouble()))

    }

    override fun getBorderInsets(c: Component?): Insets {
        //TODO: figure out the DPI aware issue and the error that follows, also for all other uses of it
        return Insets(4, 4, 4, 4)
    }

    override fun isBorderOpaque(): Boolean {
        return false
    }

//    fun drawShadow(
//        g2d: Graphics2D,
//
//        color: Color,
//    ) {
//        val shadowLayers = 5
//        val maxShadowOffset = 3.5
//        for (i in 0 until shadowLayers) {
//            val alpha = (255 * (1.0 - i.toDouble() / shadowLayers)).toInt() - 20
//            val shadowColor = Color(color.red, color.green, color.blue, alpha)
//            g2d.color = shadowColor
//            val offset = maxShadowOffset * (i.toDouble() / shadowLayers)
//            g2d.fill(Ellipse2D.Double(circle.x - offset, circle.y - offset, circle.width + 2 * offset, circle.height + 2 * offset))
//        }
////        g2d.draw(circle)
////        g2d.fill(circle)
//    }
}