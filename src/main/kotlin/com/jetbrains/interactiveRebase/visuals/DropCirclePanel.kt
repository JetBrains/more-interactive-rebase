package com.jetbrains.interactiveRebase.visuals

import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import java.awt.*
import java.awt.geom.Ellipse2D

class DropCirclePanel(private val diameter: Double,
                      private val border: Float,
                      private val color: JBColor,
                      override var commit: CommitInfo,
                      override var next: CirclePanel? = null,
                      override var previous: CirclePanel? = null,): CirclePanel(diameter, border, color, commit, next, previous) {


    override fun paintCircle(g: Graphics) {
        val g2d = g as Graphics2D
        // Set rendering hints for smoother rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        createCircle()


        g2d.color = Palette.OPAQUE
        if (commit.isSelected) {
            selectedCommitAppearance(g2d,true,Palette.OPAQUE, Palette.SELECTEDBRIGHT, Palette.BORDER)
        } else {
            selectedCommitAppearance(g2d,false,Palette.OPAQUE,Palette.DARKSHADOW, Palette.DARKBLUE)
        }
        if (commit.isHovered) {
            g2d.color = JBColor.BLACK
            g2d.stroke = BasicStroke(border)
            g2d.draw(circle)
        }
    }

    override fun drawBorder(g2d: Graphics2D, circle: Ellipse2D.Double, borderColor: Color) {
        g2d.fill(circle)
        g2d.color = borderColor

        val dashPattern = floatArrayOf(3f, 3f)
        g2d.stroke = BasicStroke(1.5f*border, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dashPattern, 0f)
        g2d.draw(circle)
    }
}