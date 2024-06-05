package com.jetbrains.interactiveRebase.visuals

import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import java.awt.Color

/**
 * Encapsulation of frequently used colors.
 */

object Palette {
    public final val BLUE = JBColor(Color(52, 152, 219), Color(41, 128, 185))
    public final val LIME = JBColor(Color(234, 217, 76), Color(192, 174, 57))
    public final val TOMATO = JBColor(Color(231, 76, 60), Color(192, 57, 43))
    public final val LIME_GREEN = JBColor(Color(81, 203, 32), Color(66, 163, 27))
    public final val INDIGO = JBColor(Color(95, 96, 202), Color(78, 79, 179))

    // TODO: figure out different colors on a more general case
    public final val DARK_BLUE = JBColor(Color(28, 115, 173, 82), Color(28, 115, 173))
    public final val GRAY = JBColor(Color(178, 172, 172), Color(65, 79, 84))
    public final val SELECTED_HIGHLIGHT = JBColor(Color(0, 0, 0), Color(120, 191, 232))
    public final val DARK_SHADOW = JBColor(Color(22, 92, 140), Color(19, 71, 139))
    public final val BLUE_BORDER = JBColor(Color(37, 47, 65), Color(21, 147, 232))
    public final val JETBRAINS_GRAY = JBColor(Gray._242, Color(60, 63, 65))
    public final val DARK_GRAY = JBColor(Color(28, 29, 31), Color(60, 63, 65))
    public final val BLUE_BUTTON = JBColor(Color(41, 152, 231), Color(20, 124, 204))
    public final val GRAY_BUTTON = JBColor(Color(178, 172, 172), Color(89, 101, 103))
    public final val WHITE_TEXT = JBColor(Color(255, 255, 255), Color(255, 255, 255))
    public final val TRANSPARENT = JBColor(Color(0, 0, 0, 0), Color(0, 0, 0, 0))
    public final val JETBRAINS_HOVER = JBColor(Color(228, 228, 230), Color(83, 83, 96))
    public final val JETBRAINS_SELECTED = JBColor(Color(215, 225, 252), Color(69, 115, 232))
}
