package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.services.ModelService
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

class RewordButtonListener(private val project: Project) : ActionListener {
    /**
     * Sets the right flag in selected commits to enable their text fields. Sets the panel to be refreshed
     * if there were any changes,meaning this action was performed while there were selected commits
     */
    override fun actionPerformed(e: ActionEvent?) {
        val modelService = project.service<ModelService>()
        modelService.branchInfo.selectedCommits.forEach {
            it.setDoubleClickedTo(true)
        }
    }
}
