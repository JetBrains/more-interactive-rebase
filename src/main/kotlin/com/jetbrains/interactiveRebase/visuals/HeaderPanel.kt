package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.listeners.reword.RewordButtonListener
import com.jetbrains.interactiveRebase.listeners.RewordClickListener
import com.jetbrains.interactiveRebase.listeners.DropCommitListener
import java.awt.BorderLayout
import java.awt.Graphics
import java.awt.*
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent

class HeaderPanel(private val mainPanel: JComponent, private val project : Project) : JBPanel<JBPanel<*>>() {
    val gitActionsPanel = JBPanel<JBPanel<*>>()
    val changeActionsPanel = JBPanel<JBPanel<*>>()

    init {
        gitActionsPanel.layout = BoxLayout(gitActionsPanel, BoxLayout.X_AXIS)
        addGitButtons(gitActionsPanel)

        changeActionsPanel.layout = BoxLayout(changeActionsPanel, BoxLayout.X_AXIS)
        addChangeButtons(changeActionsPanel)
    }

    override fun paintComponent(g: Graphics?) {
        this.layout = BorderLayout()
        super.paintComponent(g)
        this.add(gitActionsPanel, BorderLayout.WEST)
        this.add(changeActionsPanel, BorderLayout.EAST)
    }

    /**
     * Add git action buttons to the header panel.
     * At the moment, the buttons are hardcoded, but we will replace them with icons and listeners later.
     */
    private fun addGitButtons(buttonPanel: JBPanel<JBPanel<*>>) {
        val squashButton = JButton("Squash")
        val fixupButton = JButton("Stop to edit")
        val rewordButton = JButton("Reword")
        rewordButton.addActionListener(RewordButtonListener(project))
        val dropButton = JButton("Drop")
        dropButton.addMouseListener(DropCommitListener(dropButton, project))

        buttonPanel.add(squashButton)
        buttonPanel.add(fixupButton)
        buttonPanel.add(rewordButton)
        buttonPanel.add(dropButton)
    }

    /**
     * Add change action buttons to the header panel.
     * At the moment, the buttons are hardcoded, but we will replace them with icons and listeners later.
     */
    private fun addChangeButtons(buttonPanel: JBPanel<JBPanel<*>>) {
        val pickButton = JButton("Pick")
        val resetButton = JButton("Reset")

        buttonPanel.add(pickButton)
        buttonPanel.add(resetButton)
    }
}
