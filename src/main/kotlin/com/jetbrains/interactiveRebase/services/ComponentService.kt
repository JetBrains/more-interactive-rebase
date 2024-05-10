package com.jetbrains.interactiveRebase.services

import HeaderPanel
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.threads.CommitInfoThread
import com.jetbrains.interactiveRebase.visuals.LabeledBranchPanel
import com.jetbrains.interactiveRebase.visuals.Palette
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.SwingConstants

@Service(Service.Level.PROJECT)
class ComponentService(val project: Project) {
    private var mainComponent: JComponent
    private var branchInfo: BranchInfo
    private val selectedCommits: MutableList<CommitInfo>

    init {
        mainComponent = createMainComponent()
        branchInfo = BranchInfo()
        selectedCommits = mutableListOf()
    }

    fun createMainComponent(): JComponent {
        val component = JBPanel<JBPanel<*>>()
        component.layout = BorderLayout()
        return component
    }

    fun updateMainComponentThread(): JComponent{
        val thread = CommitInfoThread(project, branchInfo)
        thread.start()
        thread.join()
        updateMainPanelVisuals()
        return mainComponent
    }

    /**
     * Updates the main panel with the branch info.
     */
    fun updateMainPanelVisuals() {
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

    fun toggleCommitSelection(commit: CommitInfo) {
        if (commit.isSelected) {
            selectedCommits.add(commit)
        } else {
            selectedCommits.remove(commit)
        }
    }

    fun getSelectedCommits(): List<CommitInfo> {
        return selectedCommits.toList()
    }
}
