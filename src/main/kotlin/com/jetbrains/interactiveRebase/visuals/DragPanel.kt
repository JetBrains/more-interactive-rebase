package com.jetbrains.interactiveRebase.visuals

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import java.awt.BasicStroke
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import java.awt.geom.Path2D
import java.awt.geom.QuadCurve2D

class DragPanel : JBPanel<JBPanel<*>>() {

    var labelIsDragged: Boolean = false
    var startDragPoint: Point? = null
    var endDragPoint: Point? = null

    init {
        this.background = Palette.TRANSPARENT
        this.isOpaque = false
        this.layout = null
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        if (labelIsDragged) {
            val g2 = g as Graphics2D
            g2.stroke = BasicStroke(2f)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            if(labelIsDragged) {
                drawCurvedArrow(g2, startDragPoint!!, endDragPoint!!)
            }
        }
    }

    private fun drawCurvedArrow(g2: Graphics2D, start: Point, end: Point) {
        val controlX = (start.x + end.x) / 2
        val controlY = start.y + 80 // Adjust the curve control point as needed

        g2.color = JBColor.LIGHT_GRAY

        val curve = QuadCurve2D.Float(
            start.x.toFloat(),
            start.y.toFloat(),
            controlX.toFloat(),
            controlY.toFloat(),
            end.x.toFloat(),
            end.y.toFloat()
        )

        g2.draw(curve)

        val arrowAngle = Math.toRadians(20.0)
        val arrowLength = 8

        val angle = Math.atan2((end.y - controlY).toDouble(), (end.x - controlX).toDouble())
        val x1 = end.x - arrowLength * Math.cos(angle - arrowAngle)
        val y1 = end.y - arrowLength * Math.sin(angle - arrowAngle)
        val x2 = end.x - arrowLength * Math.cos(angle + arrowAngle)
        val y2 = end.y - arrowLength * Math.sin(angle + arrowAngle)

        val path = Path2D.Double()
        path.moveTo(end.x.toDouble(), end.y.toDouble())
        path.lineTo(x1, y1)
        path.moveTo(end.x.toDouble(), end.y.toDouble())
        path.lineTo(x2, y2)

        g2.draw(path)
    }
}
