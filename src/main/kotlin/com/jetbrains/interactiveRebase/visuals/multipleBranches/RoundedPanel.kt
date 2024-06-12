package com.jetbrains.interactiveRebase.visuals.multipleBranches

import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.visuals.Palette
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import kotlin.properties.Delegates

open class RoundedPanel : JBPanel<JBPanel<*>>() {
    internal var cornerRadius by Delegates.notNull<Int>()
    internal lateinit var backgroundColor: Color
    internal var borderColor = Palette.TRANSPARENT

    init {
        isOpaque = false
    }

    public override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g2.color = backgroundColor
        g2.fillRoundRect(0, 0, width - 2, height - 2, cornerRadius, cornerRadius)
        g2.color = borderColor
        g2.stroke = BasicStroke(1f)
        g2.drawRoundRect(0, 0, width - 2, height - 2, cornerRadius, cornerRadius)
    }
}
