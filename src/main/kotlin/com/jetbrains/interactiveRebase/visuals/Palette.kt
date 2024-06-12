package com.jetbrains.interactiveRebase.visuals

import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import java.awt.Color

/**
 * Encapsulation of frequently used colors.
 */

object Palette {
    // Specific colors
    public final val BLUE = JBColor(Color(52, 152, 219), Color(41, 128, 185))
    public final val LIME = JBColor(Color(234, 217, 76), Color(192, 174, 57))
    public final val TOMATO = JBColor(Color(231, 76, 60), Color(192, 57, 43))
    public final val LIME_GREEN = JBColor(Color(81, 203, 32), Color(66, 163, 27))
    public final val INDIGO = JBColor(Color(95, 96, 202), Color(78, 79, 179))

    // Additional colors
    public final val DARK_BLUE = JBColor(Color(28, 115, 173, 82), Color(28, 115, 173))
    public final val GRAY = JBColor(Color(178, 172, 172), Color(65, 79, 84))
    public final val SELECTED_HIGHLIGHT = JBColor(Color(0, 0, 0), Color(120, 191, 232))
    public final val DARK_SHADOW = JBColor(Color(22, 92, 140), Color(19, 71, 139))

    public final val JETBRAINS_GRAY = JBColor(Gray._242, Color(60, 63, 65))
    public final val DARK_GRAY = JBColor(Color(28, 29, 31), Color(60, 63, 65))
    public final val BLUE_BUTTON = JBColor(Color(41, 152, 231), Color(20, 124, 204))
    public final val GRAY_BUTTON = JBColor(Color(178, 172, 172), Color(89, 101, 103))
    public final val WHITE_TEXT = JBColor(Color(255, 255, 255), Color(255, 255, 255))
    public final val TRANSPARENT = JBColor(Color(0, 0, 0, 0), Color(0, 0, 0, 0))
    public final val JETBRAINS_HOVER = JBColor(Color(228, 228, 230), Color(83, 83, 96))
    public final val JETBRAINS_SELECTED = JBColor(Color(215, 225, 252), Color(69, 115, 232))
    public final val BLUE_BORDER = JBColor(Color(37, 47, 65), Color(21, 147, 232))
    public final val LIME_BORDER = JBColor(Color(195, 180, 70), Color(144, 130, 46))
    public final val TOMATO_BORDER = JBColor(Color(189, 55, 43), Color(150, 42, 33))
    public final val LIME_GREEN_BORDER = JBColor(Color(72, 180, 28), Color(58, 143, 23))

    public final val DARKER_BLUE = JBColor(BLUE.darker(), BLUE.darker())
    public final val DARKER_LIME = JBColor(LIME.darker(), LIME.darker())
    public final val DARKER_TOMATO = JBColor(Color(185, 61, 48), Color(154, 46, 35))
    public final val DARKER_LIME_GREEN = JBColor(LIME_GREEN.darker(), LIME_GREEN.darker())

    // Themes
    data class Theme(
        val regularCircleColor: JBColor,
        val borderColor: JBColor,
        val selectedCircleColor: JBColor,
        val selectedBorderColor: JBColor,
        val hoverCircleColor: JBColor,
        val reorderedCircleColor: JBColor = INDIGO,
        val reorderedBorderColor: JBColor = JBColor(INDIGO.darker(), INDIGO.darker()),
        val draggedCircleColor: JBColor = JBColor.BLUE,
    )

    public final val BLUE_THEME =
        Theme(
            regularCircleColor = BLUE,
            borderColor = DARK_BLUE,
            selectedCircleColor = DARKER_BLUE,
            selectedBorderColor = BLUE_BORDER,
            hoverCircleColor = JETBRAINS_HOVER,
        )

    public final val LIME_THEME =
        Theme(
            regularCircleColor = LIME,
            borderColor = LIME_BORDER,
            selectedCircleColor = DARKER_LIME,
            selectedBorderColor = LIME_BORDER,
            hoverCircleColor = JETBRAINS_HOVER,
        )

    public final val TOMATO_THEME =
        Theme(
            regularCircleColor = TOMATO,
            borderColor = TOMATO_BORDER,
            selectedCircleColor = DARKER_TOMATO,
            selectedBorderColor = TOMATO_BORDER,
            hoverCircleColor = JETBRAINS_HOVER,
        )

    public final val LIME_GREEN_THEME =
        Theme(
            regularCircleColor = LIME_GREEN,
            borderColor = LIME_GREEN_BORDER,
            selectedCircleColor = DARKER_LIME_GREEN,
            selectedBorderColor = LIME_GREEN_BORDER,
            hoverCircleColor = JETBRAINS_HOVER,
        )
}
