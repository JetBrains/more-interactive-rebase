package com.jetbrains.interactiveRebase.services

import HeaderPanel
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBPanel
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.threads.BranchInfoThread
import com.jetbrains.interactiveRebase.visuals.CommitInfoPanel
import com.jetbrains.interactiveRebase.visuals.LabeledBranchPanel
import com.jetbrains.interactiveRebase.visuals.Palette
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.SwingConstants

@Service(Service.Level.PROJECT)
class ComponentService(val project: Project) {
    var mainComponent: JComponent
    var branchInfo: BranchInfo
    var commitInfoPanel = CommitInfoPanel(project)

    init {
        mainComponent = createMainComponent()
        branchInfo = BranchInfo()
        refresh()
    }

    /**
     * Initializes the main component.
     */
    fun createMainComponent(): JComponent {
        val component = JBPanel<JBPanel<*>>()
        component.layout = BorderLayout()
        return component
    }

    /**
     * Calls the BranchInfoThread to update the branch info.
     */

    fun refresh(): JComponent {
        updateBranchInfo()
        updateMainPanelVisuals()
        return mainComponent
    }

    /**
     * Gets mainComponent
     */

    fun getComponent(): JComponent {
        val name = branchInfo.name
        updateBranchInfo()
        if (name != branchInfo.name)
            {
                updateMainPanelVisuals()
            }
        return mainComponent
    }

    /**
     * Updates the main panel visual elements with the updated branch info.
     */
    fun updateMainPanelVisuals() {
        mainComponent.removeAll()
        val headerPanel = HeaderPanel(mainComponent)

        val branchPanel = createBranchPanel()

        val firstDivider =
            OnePixelSplitter(false, 0.7f).apply {
                firstComponent = branchPanel
                secondComponent = commitInfoPanel
            }

        val secondDivider =
            OnePixelSplitter(true, 0.03f, 0.03f, 0.03f).apply {
                firstComponent = headerPanel
                secondComponent = firstDivider
            }

        mainComponent.add(secondDivider, BorderLayout.CENTER)
    }

    /**
     * Creates a branch panel with the given branch info.
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

    /**
     * Adds/removes the given commit to the selected commits list.
     */
    fun addOrRemoveCommitSelection(commit: CommitInfo) {
        if (commit.isSelected) {
            branchInfo.selectedCommits.add(commit)
        } else {
            branchInfo.selectedCommits.remove(commit)
        }
        this.commitInfoPanel.commitsSelected(branchInfo.selectedCommits.map { it.commit })
        commitInfoPanel.repaint()
    }

    /**
     * Returns the selected commits.
     */
    fun getSelectedCommits(): List<CommitInfo> {
        return branchInfo.selectedCommits.toList()
    }

    companion object {
        @Volatile
        private var instance: ComponentService? = null

        fun getInstance(project: Project): ComponentService {
            return instance ?: synchronized(this) {
                instance ?: ComponentService(project).also { instance = it }
            }
        }
    }

    @RequiresBackgroundThread
    fun updateBranchInfo() {
        val thread = BranchInfoThread(project, branchInfo)
        thread.start()
        thread.join()
    }
}
