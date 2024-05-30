package com.jetbrains.interactiveRebase.visuals.multipleBranches

import com.intellij.ui.components.JBPanel
import java.awt.GridBagConstraints
import java.awt.GridBagLayout

class SidePanel: JBPanel<JBPanel<*>>() {
    internal var isVisible: Boolean = false
    internal var branches: MutableList<String> = mutableListOf()

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
        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = i
        gbc.weightx = 0.0
        gbc.weighty = if (i == branches.size - 1) 1.0 else 0.0
        gbc.anchor = GridBagConstraints.NORTH
        gbc.fill = GridBagConstraints.HORIZONTAL
        add(branch, gbc)
    }
}