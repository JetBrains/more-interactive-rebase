package com.jetbrains.interactiveRebase.actions.GitPanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.interactiveRebase.services.ModelService

class RewordAction : DumbAwareAction("Reword", "Reword a commit", AllIcons.Actions.SuggestedRefactoringBulb) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        if(project != null) {
            val modelService = project.service<ModelService>()
            modelService.branchInfo.selectedCommits.forEach {
                it.setDoubleClickedTo(true)
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
//        println("updated")
        e.presentation.isEnabled = true
        e.presentation.isEnabledAndVisible = true
//        e.presentation.
//        super.update(e)
    }
}