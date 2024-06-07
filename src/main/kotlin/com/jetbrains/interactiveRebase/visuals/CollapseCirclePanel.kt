package com.jetbrains.interactiveRebase.visuals

import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import icons.VcsLogIcons
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints

class CollapseCirclePanel(diameter: Double,
                          private val border: Float,
                          color: JBColor,
                          override var commit: CommitInfo,
                          override var next: CirclePanel? = null,
                          override var previous: CirclePanel? = null,):
    CirclePanel(diameter, border, color, commit, next, previous) {
    override fun paintCircle(g: Graphics) {
        val g2d = g as Graphics2D

        // Set rendering hints for smoother rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        createCircle(diameter)
        val circleColor = parent.background
        val borderColor = parent.background
        selectedCommitAppearance(g2d, commit.isSelected, circleColor, borderColor)
//
//        if (commit.isHovered) {
//            g2d.color = JBColor.BLACK
//            g2d.stroke = BasicStroke(border)
//            g2d.draw(circle)
//        }

        // TODO: Very hard to unit test, icon cannot be mocked
        paintIcon(g2d)
    }

    fun paintIcon(g: Graphics) {
//         val icon = PlatformIcons.EDIT
        val icon = RotatedIcon(VcsLogIcons.Process.Dots_1, 90.0)
//        val icon = VcsLogIcons.Process.Dots_1
        val iconX = (width - icon.iconWidth) / 2
        val iconY = (height - icon.iconHeight) / 2
        icon.paintIcon(this, g, iconX, iconY)
    }
}