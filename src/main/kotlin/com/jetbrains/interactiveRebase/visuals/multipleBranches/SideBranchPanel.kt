package com.jetbrains.interactiveRebase.visuals.multipleBranches

import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import java.awt.Color
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JButton

class SideBranchPanel(val branchName: String): JBPanel<JBPanel<*>>() {
    var isSelected: Boolean = false
    init {
        layout = GridBagLayout()

        val label = JBLabel(branchName)


        val button = JButton()
        button.icon = AllIcons.General.Remove
        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.9 // Label takes up entire space in the x-direction
        gbc.anchor = GridBagConstraints.LINE_START // Label is aligned to the left
        gbc.insets = Insets(5, 20, 5, 5) // Padding

        add(label, gbc)

        gbc.gridx = 1
        gbc.weightx = 0.1 // Button takes up minimal space in the x-direction
        gbc.anchor = GridBagConstraints.LINE_END // Button is aligned to the right
        button.border = null // Remove button border
        val iconSize = button.icon?.let { Dimension(it.iconWidth, it.iconHeight) }
        button.preferredSize = iconSize
        button.isOpaque = false
        button.setContentAreaFilled(false)
        gbc.insets = Insets(5, 5, 5, 15)
        add(button, gbc)
        border = BorderFactory.createMatteBorder(0, 0, 1, 0, Color(200,200,200, 50))

        this.minimumSize = Dimension(300, 50)
        this.preferredSize = Dimension(300, 50)
    }
//
//    override fun paintComponent(g: Graphics?) {
//        super.paintComponent(g)
//        val g2d = g as Graphics2D
//    }
}