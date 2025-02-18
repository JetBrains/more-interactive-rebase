package com.jetbrains.interactiveRebase.visuals.multipleBranches

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.jetbrains.interactiveRebase.listeners.RemoveSideBranchListener
import com.jetbrains.interactiveRebase.listeners.SideBranchPanelListener
import com.jetbrains.interactiveRebase.listeners.SidePanelListener
import com.jetbrains.interactiveRebase.services.ModelService
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.SwingUtilities

class SidePanel(var branches: MutableList<String>, val project: Project) : JBPanel<JBPanel<*>>() {
    internal var isVisible: Boolean = false
    internal var listener: SidePanelListener = SidePanelListener(project, this)

    var sideBranchPanels: MutableList<SideBranchPanel> = mutableListOf()

    init {
        layout = GridBagLayout()

        updateBranchNames()
        addKeyListener(listener)
    }

    /**
     * Sets the actual visibility of the Swing component based on the flag
     */
    override fun setVisible(aFlag: Boolean) {
        super.setVisible(aFlag)
        revalidate()
        repaint()
    }

    /**
     * Updates the branch names in the panel. Clears all panels and redraws, if a branch is selected, does
     * the required styling
     */
    fun updateBranchNames() {
        removeAll()
        sideBranchPanels.clear()
        updateBranches()
        val addedBranch = project.service<ModelService>().graphInfo.addedBranch?.name
        var selectedPanel: SideBranchPanel? = null
        for (i in 0 until branches.size) {
            createSideBranchPanel(i)
            val panel: SideBranchPanel = sideBranchPanels[i]

            if (panel.branchName == addedBranch) {
                panel.selectBranchVisually()
                selectedPanel = panel
            }
        }
        if (selectedPanel != null) {
            makeBranchesUnavailableExceptCurrent(selectedPanel)
        }

        revalidate()
        repaint()
    }

    /**
     * Updates the branches in the panel.
     */
    fun updateBranches() {
        branches = project.service<ModelService>().graphInfo.branchList
    }

    /**
     * Creates a panel for a single branch name.
     */
    fun createSideBranchPanel(i: Int) {
        val branch = SideBranchPanel(branches[i], project)
        sideBranchPanels.add(branch)

        addBranchListener(branch)
        addRemoveBranchButtonListener(branch)

        val gbc = getAlignmentForBranch(i)
        add(branch, gbc)
    }

    /**
     * Adds a listener to the branch panel.
     */
    fun addBranchListener(branch: SideBranchPanel) {
        val sideBranchPanelListener = SideBranchPanelListener(branch, this)
        branch.addMouseListener(sideBranchPanelListener)
        branch.addMouseMotionListener(sideBranchPanelListener)
        Disposer.register(branch, sideBranchPanelListener)
    }

    /**
     * Adds a listener to the remove button of the branch panel.
     */
    fun addRemoveBranchButtonListener(branch: SideBranchPanel) {
        val removeListener = RemoveSideBranchListener(branch, this)
        branch.button.addMouseListener(removeListener)
        branch.button.addMouseMotionListener(removeListener)
    }

    /**
     * Returns the alignment for the button in the panel.
     */
    fun getAlignmentForBranch(i: Int): GridBagConstraints {
        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = i
        gbc.weightx = 1.0
        gbc.weighty = if (i == branches.size - 1) 1.0 else 0.0
        gbc.anchor = GridBagConstraints.NORTH
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = JBUI.insets(2, 4, 1, 4)

        return gbc
    }

    /**
     * Checks if the branch can be selected,
     * which mimics the idea of a radio button.
     */
    fun canSelectBranch(sideBranchPanel: SideBranchPanel): Boolean {
        if (sideBranchPanel.isSelected) {
            return false
        }
        for (branch in sideBranchPanels) {
            if (branch != sideBranchPanel && branch.isSelected) {
                return false
            }
        }
        return true
    }

    /**
     * Grays out all branches, making them look
     * unavailable to select except the current one.
     */
    fun makeBranchesUnavailableExceptCurrent(sideBranchPanel: SideBranchPanel) {
        for (branch in sideBranchPanels) {
            if (branch != sideBranchPanel) {
                branch.grayOutText()
            }
        }
    }

    /**
     * Resets all branches to their original state.
     */
    fun resetAllBranchesVisually() {
        for (branch in sideBranchPanels) {
            branch.resetSideBranchPanelVisually()
        }
    }

    fun selectBranch(sideBranchPanel: SideBranchPanel) {
        if (canSelectBranch(sideBranchPanel)) {
            // make rest of the branches unavailable if branch is added successfully
            if (sideBranchPanel.selectBranch()) {
                makeBranchesUnavailableExceptCurrent(sideBranchPanel)
                listener.selected = sideBranchPanel
            }
            return
        }
        if (sideBranchPanel.isSelected) {
            // We possibly don't want to unselect the branch only by a single click on the panel, TBD
            // if deselecting was successful then reset all branches visually
            if (sideBranchPanel.deselectBranch()) resetAllBranchesVisually()
        }
    }

    /**
     * Gives focus to the panel
     */
    fun selectPanel() {
        SwingUtilities.invokeLater { requestFocusInWindow() }
        background = JBColor.LIGHT_GRAY
        if (listener.selected == null) {
            sideBranchPanels.first().onHover()
            listener.selected = sideBranchPanels.first()
        }
    }
}
