package com.jetbrains.interactiveRebase.visuals.multipleBranches

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.visuals.Palette
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.LinearGradientPaint
import java.awt.RenderingHints
import kotlin.properties.Delegates

open class RoundedPanel : JBPanel<JBPanel<*>>() {
    internal var cornerRadius by Delegates.notNull<Int>()
    internal var backgroundColor: Color = JBColor.BLUE
    internal var borderColor = Palette.TRANSPARENT
    private var borderGradientColors: MutableList<Color> = mutableListOf()
    private var backgroundGradientColors: MutableList<Color> = mutableListOf()

    init {
        isOpaque = false
        cornerRadius = 15
    }

    public override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2 = g as Graphics2D

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        g2.color = backgroundColor
        if (backgroundGradientColors.isNotEmpty()) {
            g2.paint = createGradient(backgroundGradientColors)
        }
        g2.fillRoundRect(0, 0, width - 2, height - 2, cornerRadius, cornerRadius)
        g2.color = borderColor
        if (borderGradientColors.isNotEmpty()) {
            g2.paint = createGradient(borderGradientColors)
        }
        g2.stroke = BasicStroke(1.5f)
        g2.drawRoundRect(0, 0, width - 3, height - 3, cornerRadius, cornerRadius)
    }

    fun addBorderGradient(topColor: Color, bottomColor: Color) {
        borderGradientColors = mutableListOf(topColor, bottomColor)
        borderColor = Palette.TRANSPARENT
    }

    fun removeBorderGradient() {
        borderGradientColors = mutableListOf()
    }

    fun addBackgroundGradient(topColor: Color, bottomColor: Color) {
        backgroundColor = Palette.TRANSPARENT
        backgroundGradientColors = mutableListOf(topColor, bottomColor)
    }

    fun removeBackgroundGradient() {
        backgroundGradientColors = mutableListOf()
    }

    private fun createGradient(gradientColors: MutableList<Color>): LinearGradientPaint {
        return LinearGradientPaint(
            width / 2f,
            0f,
            width / 2f,
            height.toFloat(),
            floatArrayOf(0.2f, 0.8f),
            gradientColors.toTypedArray(),
        )
    }
}
