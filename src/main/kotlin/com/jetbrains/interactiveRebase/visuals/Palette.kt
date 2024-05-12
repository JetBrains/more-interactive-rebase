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
    public final val LIME_GREEN = JBColor(Color(81, 203, 32), Color(66,163,27))

    // TODO: figure out different colors on a more general case
    public final val DARKBLUE = JBColor(Color(28, 115, 173, 82), Color(28, 115, 173))
    public final val GRAY = JBColor(Color(178, 172, 172), Color(65, 79, 84))
    public final val SELECTEDHIGHLIGHT = JBColor(Color(0, 0, 0), Color(120, 191, 232))
    public final val DARKSHADOW = JBColor(Color(22, 92, 140), Color(19, 71, 139))
    public final val BLUEBORDER = JBColor(Color(37, 47, 65), Color(21, 147, 232))
    public final val JETBRAINSGRAY = JBColor(Gray._242, Color(60, 63, 65))
    public final val DARKGRAY = JBColor(Color(28, 29, 31), Color(60, 63, 65))
}
