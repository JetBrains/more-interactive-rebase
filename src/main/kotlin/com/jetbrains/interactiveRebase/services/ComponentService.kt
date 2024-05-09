package com.jetbrains.interactiveRebase.services

import HeaderPanel
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.visuals.LabeledBranchPanel
import com.jetbrains.interactiveRebase.visuals.Palette
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.SwingConstants

class ComponentService(val mainComponent: JComponent, val branchInfo: BranchInfo) {
    /**
     * Updates the main panel with the branch info.
     */
    fun updateMainPanel() {
        mainComponent.removeAll()
        val headerPanel = HeaderPanel(mainComponent)
        mainComponent.add(headerPanel, BorderLayout.NORTH)

        val branchPanel = createBranchPanel()
        mainComponent.add(branchPanel, BorderLayout.CENTER)
    }

    /**
     * Creates a branch panel with the branch info.
     */
    fun createBranchPanel(): JBPanel<JBPanel<*>> {
        val branchPanel = JBPanel<JBPanel<*>>()
        branchPanel.layout = GridBagLayout()
        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.fill = GridBagConstraints.BOTH
        branchPanel.add(
            LabeledBranchPanel(
                branchInfo,
                Palette.BLUE,
                SwingConstants.RIGHT,
            ),
            gbc,
        )
        return branchPanel
    }
}
