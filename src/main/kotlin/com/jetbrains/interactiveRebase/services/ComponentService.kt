package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.threads.BranchInfoThread
import com.jetbrains.interactiveRebase.visuals.CommitInfoPanel
import com.jetbrains.interactiveRebase.visuals.HeaderPanel
import com.jetbrains.interactiveRebase.visuals.LabeledBranchPanel
import com.jetbrains.interactiveRebase.visuals.Palette
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.SwingConstants

@Service(Service.Level.PROJECT)
class ComponentService(val project: Project) {
    var branchInfo = BranchInfo()
    var mainPanel = JBPanel<JBPanel<*>>()
    private var commitInfoPanel = CommitInfoPanel(project)
    private var contentPanel: JBPanel<JBPanel<*>>
    private var branchPanel: LabeledBranchPanel
    var isDirty = false

    init {
        branchPanel = createBranchPanel()
        contentPanel = createContentPanel()

        this.mainPanel.layout = BorderLayout()
        renderMainPanel()
    }

    /**
     * Initializes the main component.
     */
    fun renderMainPanel(): JComponent {
        mainPanel.removeAll()
        val headerPanel = HeaderPanel(mainPanel, project)

        val firstDivider =
            OnePixelSplitter(false, 0.7f).apply {
                firstComponent = contentPanel
                secondComponent = commitInfoPanel
            }

        val secondDivider =
            OnePixelSplitter(true, 0.03f, 0.03f, 0.03f).apply {
                firstComponent = headerPanel
                secondComponent = firstDivider
                setResizeEnabled(false)
            }

        mainPanel.add(secondDivider, BorderLayout.CENTER)

        return mainPanel
    }

    /**
     * Creates a content panel.
     */
    fun createContentPanel(): JBPanel<JBPanel<*>> {
        val contentPanel = JBPanel<JBPanel<*>>()
        contentPanel.layout = GridBagLayout()

        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.fill = GridBagConstraints.BOTH

        contentPanel.add(
            branchPanel,
            gbc,
        )
        return contentPanel
    }

    /**
     * Creates a branch panel.
     */

    fun createBranchPanel(): LabeledBranchPanel {
        return LabeledBranchPanel(
            branchInfo,
            Palette.BLUE,
            SwingConstants.RIGHT,
        )
    }

    /**
     * Gets mainPanel
     */
    fun getComponent(): JComponent {
        fetchBranchInfo()
        if (this.isDirty) {
            updateMainPanel()
        }
        return mainPanel
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
        updateCommitInfoPanel()
    }

    /**
     * Returns the selected commits.
     */
    fun getSelectedCommits(): List<CommitInfo> {
        return branchInfo.selectedCommits.toList()
    }

    fun clearAllSelectedCommits(){
        branchInfo.selectedCommits.forEach {
            it.isSelected = false
        }
        branchInfo.selectedCommits.clear()
        this.commitInfoPanel.commitsSelected(listOf())
        commitInfoPanel.repaint()
    }

//    fun getSelectedCirclePanels(labeledBranchPanel: LabeledBranchPanel): MutableList<CirclePanel> {
//        val circles = labeledBranchPanel.branchPanel.circles
//        val list : MutableList<CirclePanel> = mutableListOf()
//        for(circle in circles) {
//            if (circle.commit.isSelected) {
//                list.add(circle)
//                println(circle.commit.getSubject())
//            }
//        }
//        return list
//    }
//
//    fun getLabeledBranchPanel(): LabeledBranchPanel {
//        val secondDivider = mainComponent.getComponent(0) as OnePixelSplitter
//        val branchPanelSplitter = secondDivider.secondComponent as OnePixelSplitter
//        val branchPanel = branchPanelSplitter.firstComponent
//        return branchPanel.getComponent(0) as LabeledBranchPanel
//    }

    /**
     * Calls thread to update branch info
     */

    fun fetchBranchInfo() {
        val thread = BranchInfoThread(project, branchInfo)
        thread.start()
    }

    /**
     * Updates and repaints the commit info panel
     */

    fun updateCommitInfoPanel() {
        this.commitInfoPanel.commitsSelected(branchInfo.selectedCommits.map { it.commit })
        commitInfoPanel.repaint()
    }

    /**
     * Repaints the branch panel
     */

    fun updateBranchPanel() {
        branchPanel = createBranchPanel()
        contentPanel = createContentPanel()
    }

    /**
     * Updates the main component
     */

    fun updateMainPanel() {
        updateBranchPanel()
        updateCommitInfoPanel()
        renderMainPanel()
        isDirty = false
    }
}
