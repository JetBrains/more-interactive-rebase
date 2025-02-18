package com.jetbrains.interactiveRebase.visuals

import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import java.awt.Color

/**
 * Encapsulation of frequently used colors.
 */

object Palette {
    // Specific colors
    val BLUE = JBColor(Color(52, 152, 219), Color(41, 128, 185))
    val LIME = JBColor(Color(234, 217, 76), Color(192, 174, 57))
    val TOMATO = JBColor(Color(231, 76, 60), Color(192, 57, 43))
    val TOMATO_DARK = JBColor(Color(92, 14, 13), Color(92, 14, 13))
    val LIME_GREEN = JBColor(Color(81, 203, 32), Color(66, 163, 27))
    val INDIGO = JBColor(Color(95, 96, 202), Color(78, 79, 179))

    // Additional colors
    val DARK_BLUE = JBColor(Color(28, 115, 173, 82), Color(28, 115, 173))
    val GRAY = JBColor(Color(178, 172, 172), Color(65, 79, 84))
    val SELECTED_HIGHLIGHT = JBColor(Color(0, 0, 0), Color(120, 191, 232))
    val DARK_SHADOW = JBColor(Color(22, 92, 140), Color(19, 71, 139))
    val JETBRAINS_GRAY = JBColor(Gray._242, Color(60, 63, 65))
    val DARK_GRAY = JBColor(Color(28, 29, 31), Color(60, 63, 65))
    val BLUE_BUTTON = JBColor(Color(41, 152, 231), Color(20, 124, 204))
    val GRAY_BUTTON = JBColor(Color(178, 172, 172), Color(89, 101, 103))
    val WHITE_TEXT = JBColor(Color(255, 255, 255), Color(255, 255, 255))
    val TRANSPARENT = JBColor(Color(0, 0, 0, 0), Color(0, 0, 0, 0))
    val JETBRAINS_HOVER = JBColor(Color(228, 228, 230), Color(83, 83, 96))
    val JETBRAINS_SELECTED = JBColor(Color(215, 225, 252), Color(69, 115, 232))

    val BLUE_BORDER = JBColor(Color(37, 47, 65), Color(21, 147, 232))
    val LIME_BORDER = JBColor(Color(195, 180, 70), Color(144, 130, 46))
    val TOMATO_BORDER = JBColor(Color(189, 55, 43), Color(150, 42, 33))
    val LIME_GREEN_REGULAR = JBColor(Color(72, 180, 28), Color(58, 143, 23))
    val LIME_GREEN_BORDER = JBColor(Color(51, 119, 23), Color(49, 106, 21))

    val DARKER_BLUE = JBColor(BLUE.darker(), BLUE.darker())
    val DARKER_LIME = JBColor(LIME.darker(), LIME.darker())
    val DARKER_TOMATO = JBColor(Color(185, 61, 48), Color(154, 46, 35))
    val DARKER_LIME_GREEN = JBColor(LIME_GREEN.darker(), LIME_GREEN.darker())

    val LIGHT_BLUE = JBColor(Color(127, 181, 218), Color(107, 171, 214))
    val PALE_BLUE = JBColor(Color(52, 152, 219, 85), Color(41, 128, 185, 85))
    val PALE_LIME = JBColor(Color(234, 217, 76, 85), Color(192, 174, 57, 85))
    val PALE_TOMATO = JBColor(Color(231, 76, 60, 85), Color(192, 57, 43, 85))
    val PALE_LIME_GREEN = JBColor(Color(81, 203, 32, 85), Color(66, 163, 27, 85))
    val FADED_LIME_GREEN = JBColor(Color(45, 113, 18), Color(45, 113, 18))
    val PALE_GREEN = JBColor(Color(39, 96, 15), Color(39, 96, 15))

    // Themes
    data class Theme(
        val regularCircleColor: JBColor,
        val borderColor: JBColor,
        val selectedCircleColor: JBColor,
        val selectedBorderColor: JBColor,
        val hoverCircleColor: JBColor,
        val branchNameColor: JBColor,
        val reorderedCircleColor: JBColor = regularCircleColor,
        val reorderedBorderColor: JBColor = borderColor,
        val draggedCircleColor: JBColor = JBColor.BLUE,
    )

    val BLUE_THEME =
        Theme(
            regularCircleColor = BLUE,
            borderColor = DARK_BLUE,
            selectedCircleColor = DARKER_BLUE,
            selectedBorderColor = BLUE_BORDER,
            hoverCircleColor = JETBRAINS_HOVER,
            branchNameColor = PALE_BLUE,
        )

    val BLUE_THEME_LIGHT =
        Theme(
            regularCircleColor = LIGHT_BLUE,
            borderColor = DARK_BLUE,
            selectedCircleColor = DARKER_BLUE,
            selectedBorderColor = BLUE_BORDER,
            hoverCircleColor = JETBRAINS_HOVER,
            branchNameColor = PALE_BLUE,
        )

    val LIME_THEME =
        Theme(
            regularCircleColor = LIME,
            borderColor = LIME_BORDER,
            selectedCircleColor = DARKER_LIME,
            selectedBorderColor = LIME_BORDER,
            hoverCircleColor = JETBRAINS_HOVER,
            branchNameColor = PALE_LIME,
        )

    val TOMATO_THEME =
        Theme(
            regularCircleColor = TOMATO,
            borderColor = TOMATO_BORDER,
            selectedCircleColor = DARKER_TOMATO,
            selectedBorderColor = TOMATO_BORDER,
            hoverCircleColor = JETBRAINS_HOVER,
            branchNameColor = PALE_TOMATO,
        )

    val LIME_GREEN_THEME =
        Theme(
            regularCircleColor = LIME_GREEN_REGULAR,
            borderColor = LIME_GREEN_BORDER,
            selectedCircleColor = DARKER_LIME_GREEN,
            selectedBorderColor = LIME_GREEN_BORDER,
            hoverCircleColor = JETBRAINS_HOVER,
            branchNameColor = PALE_LIME_GREEN,
        )

    val FADED_LIME_GREEN_THEME =
        Theme(
            regularCircleColor = PALE_GREEN,
            borderColor = PALE_LIME_GREEN,
            selectedCircleColor = DARKER_LIME_GREEN,
            selectedBorderColor = LIME_GREEN_BORDER,
            hoverCircleColor = JETBRAINS_HOVER,
            branchNameColor = PALE_LIME_GREEN,
        )

    val GRAY_THEME =
        Theme(
            regularCircleColor = GRAY,
            borderColor = DARK_GRAY,
            selectedCircleColor = GRAY.darker() as JBColor,
            selectedBorderColor = DARK_GRAY.darker() as JBColor,
            hoverCircleColor = JETBRAINS_HOVER,
            branchNameColor = GRAY,
            draggedCircleColor = GRAY,
        )
}
