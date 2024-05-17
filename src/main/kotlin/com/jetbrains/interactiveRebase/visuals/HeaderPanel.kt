package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel

import com.jetbrains.interactiveRebase.listeners.DropCommitListener
import com.jetbrains.interactiveRebase.listeners.RewordButtonListener

import com.intellij.vcs.log.impl.VcsCommitMetadataImpl
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo

import com.jetbrains.interactiveRebase.dataClasses.commands.RewordCommand
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import com.jetbrains.interactiveRebase.utils.gitUtils.IRGitRebaseUtils
import git4idea.GitCommit
import git4ideaClasses.GitRebaseEntryGeneratedUsingLog
import git4ideaClasses.IRCommitsTable
import git4ideaClasses.IRGitEntry
import git4ideaClasses.IRGitModel
import java.awt.BorderLayout
import java.awt.Graphics
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent


class HeaderPanel(private val mainPanel: JComponent, private val project: Project) : JBPanel<JBPanel<*>>() {

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
