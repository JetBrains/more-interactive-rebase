package com.jetbrains.interactiveRebase.actions.GitPanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.jetbrains.interactiveRebase.services.ModelService

class StopToEditAction : AnAction("Stop to Edit", "Pause the rebasing action to edit a commit", AllIcons.Actions.Pause) {
    override fun actionPerformed(e: AnActionEvent) {
        println("stop to edit")
        TODO("Not yet implemented")
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = true
        val project = e.project
        if (project != null && project.service<ModelService>().branchInfo.selectedCommits.size < 1) {
            e.presentation.isEnabled = false
        }
//        super.update(e)
    }

}