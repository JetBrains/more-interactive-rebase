package com.jetbrains.interactiveRebase.visuals.multipleBranches

import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.jetbrains.interactiveRebase.listeners.RemoveSideBranchListener
import com.jetbrains.interactiveRebase.listeners.SideBranchPanelListener
import java.awt.GridBagConstraints
import java.awt.GridBagLayout

class SidePanel: JBPanel<JBPanel<*>>() {
    internal var isVisible: Boolean = false
    internal var branches: MutableList<String> = mutableListOf()
    internal var sideBranchPanels: MutableList<SideBranchPanel> = mutableListOf()

    init {
        layout = GridBagLayout()
        branches = mutableListOf("Branch 1", "Branch 2", "Branch 3")
        updateBranchNames()
    }

    /**
     * Sets the actual visibility of the Swing component based on the flag
     */
    override fun setVisible(aFlag: Boolean) {
        super.setVisible(aFlag)
        revalidate()
        repaint()
    }

    fun updateBranchNames(){
        for (i in 0 until branches.size){
            createSideBranchPanel(i)
        }
    }

    fun createSideBranchPanel(i: Int) {
        val branch = SideBranchPanel(branches[i])

        val sideBranchPanelListener = SideBranchPanelListener(branch, this)
        branch.addMouseListener(sideBranchPanelListener)
        branch.addMouseMotionListener(sideBranchPanelListener)
        Disposer.register(branch, sideBranchPanelListener)

         val removeListener = RemoveSideBranchListener(branch, this)
        branch.button.addMouseListener(removeListener)
        branch.button.addMouseMotionListener(removeListener)

        sideBranchPanels.add(branch)
        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = i
        gbc.weightx = 1.0
        gbc.weighty = if (i == branches.size - 1) 1.0 else 0.0
        gbc.anchor = GridBagConstraints.NORTH
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = JBUI.insets(2,4,1,4)
        add(branch, gbc)
    }

    fun canSelectBranch(sideBranchPanel: SideBranchPanel): Boolean{
        if(sideBranchPanel.isSelected)
            return false
        for(branch in sideBranchPanels){
            if(branch != sideBranchPanel && branch.isSelected)
                return false
        }
        return true
    }

    fun makeBranchesUnavailableExceptCurrent(sideBranchPanel: SideBranchPanel){
        for(branch in sideBranchPanels){
            if(branch != sideBranchPanel)
                branch.grayOutText()
        }
    }

    fun resetAllBranchesVisually(){
        for(branch in sideBranchPanels){
            branch.resetSideBranchPanelVisually()
        }
    }
}