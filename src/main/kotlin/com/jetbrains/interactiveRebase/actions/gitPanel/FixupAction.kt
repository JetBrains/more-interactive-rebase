package com.jetbrains.interactiveRebase.actions.gitPanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.interactiveRebase.services.ModelService

class FixupAction : DumbAwareAction("Fixup", "Combine commits and set a default message", AllIcons.Actions.ListFiles) {
    override fun actionPerformed(e: AnActionEvent) {
        println("fixup")
        TODO("Not yet implemented")
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = true
        val project = e.project
        if (project != null && project.service<ModelService>().branchInfo.selectedCommits.size < 1) {
            e.presentation.isEnabled = false
        }
//        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}
