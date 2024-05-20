package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.components.service
import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.services.ComponentService
import java.awt.BasicStroke
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Insets
import java.awt.RenderingHints
import javax.swing.JTextField
import javax.swing.border.AbstractBorder

class RoundedTextField(private val commitInfo: CommitInfo, inputText: String, private val borderColor: JBColor) : JTextField(inputText) {
    private val componentService = commitInfo.project.service<ComponentService>()

    init {
        isOpaque = true
        background = background.darker()
        border = RoundedBorder(borderColor)
        text = inputText
        isFocusable = true
        border = RoundedBorder(borderColor)
    }

    /**
     * Sets the right flags to make the text field invisible again
     */
    fun exitTextBox() {
        commitInfo.isDoubleClicked = false
        componentService.branchInfo.selectedCommits.remove(commitInfo)
        commitInfo.isSelected = false
        componentService.isDirty = true
    }
}

class RoundedBorder(private val color: JBColor) : AbstractBorder() {
    override fun paintBorder(
        c: Component?,
        g: Graphics?,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
    ) {
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.color = color
        val arc = 8
        g2.stroke = BasicStroke(2.5f)
        g2.drawRoundRect(x, y, width, height, arc, arc)
    }

    override fun getBorderInsets(c: Component?): Insets {
        // TODO: figure out the DPI aware issue and the error that follows, also for all other uses of it
        return Insets(4, 4, 4, 4)
    }

    override fun isBorderOpaque(): Boolean {
        return false
    }
}
