package com.jetbrains.interactiveRebase.visuals

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.visuals.Palette.JETBRAINSGRAY
import java.awt.BasicStroke
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints

class StopToEditCirclePanel(
    private val diameter: Double,
    private val border: Float,
    private val color: JBColor,
    override var commit: CommitInfo,
    override var next: CirclePanel? = null,
    override var previous: CirclePanel? = null,
) : CirclePanel(diameter, border, color, commit, next, previous) {
    override fun paintCircle(g: Graphics) {
        val g2d = g as Graphics2D

        // Set rendering hints for smoother rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        createCircle()
        val circleColor = if (commit.isSelected) Palette.DARKGRAY.darker() else JETBRAINSGRAY
        val borderColor = if (commit.isSelected) Palette.BLUEBORDER.darker() else Palette.DARKBLUE
        selectedCommitAppearance(g2d, commit.isSelected, circleColor, borderColor)

        if (commit.isHovered) {
            g2d.color = JBColor.BLACK
            g2d.stroke = BasicStroke(border)
            g2d.draw(circle)
        }
//        val icon = PlatformIcons.EDIT
        val icon = AllIcons.Actions.Pause
        val iconX = (width - icon.iconWidth) / 2
        val iconY = (height - icon.iconHeight) / 2
        icon.paintIcon(this, g, iconX, iconY)
    }
}
