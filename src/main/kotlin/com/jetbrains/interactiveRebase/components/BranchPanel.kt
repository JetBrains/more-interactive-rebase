package com.jetbrains.interactiveRebase.components

import CirclePanel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import java.awt.*

class BranchPanel(private val commitMessages: List<String>) : JBPanel<JBPanel<*>>() {

    private val circleDiameter = 20
    private val circleGap = 30
    private val border = 1f

    private val circles: MutableList<CirclePanel> = mutableListOf()

    init {
        preferredSize = Dimension(100, 600)
        for (i in commitMessages.indices) {
            val circle = CirclePanel(circleDiameter.toDouble(), border)
            circles.add(circle)
        }
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        removeAll()

        var y = circleGap
        for (i in circles.indices) {
            val x = (width - circleDiameter) / 2
            if (i < circles.size - 1) {
                g2d.stroke = BasicStroke(border)
                g2d.color = JBColor.BLACK
                g2d.drawLine(x + circleDiameter / 2, y + circleDiameter / 2, x + circleDiameter / 2, y + circleGap + circleDiameter)
            }
            val circle = circles[i]
            circle.setBounds(x, y, circleDiameter, circleDiameter)
            add(circle)
            y += circleDiameter + circleGap
        }
    }

    override fun repaint() {
        super.repaint()
        for (component in components) {
            component.repaint()
        }
    }
}
