package com.jetbrains.interactiveRebase.actions.GitPanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.services.ComponentService
import com.jetbrains.interactiveRebase.services.ModelService

class DropAction: AnAction("Drop", "Remove a commit", AllIcons.Actions.DeleteTagHover) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        if (project != null) {
            val service = project.service<ModelService>()
            service.getSelectedCommits().forEach {
                    commitInfo ->
                commitInfo.addChange(DropCommand(commitInfo))
            }
            service.branchInfo.clearSelectedCommits()
        }

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