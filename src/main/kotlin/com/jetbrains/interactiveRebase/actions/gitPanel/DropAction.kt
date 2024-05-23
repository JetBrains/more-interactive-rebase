package com.jetbrains.interactiveRebase.actions.gitPanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.services.RebaseInvoker

class DropAction : DumbAwareAction("Drop", "Remove a commit", AllIcons.Actions.DeleteTagHover) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        if (project != null) {
            val modelService = project.service<ModelService>()
            val invoker = project.service<RebaseInvoker>()
            val commits = modelService.getSelectedCommits()
            commits.forEach {
                    commitInfo ->
                commitInfo.addChange(DropCommand(mutableListOf(commitInfo)))
            }
            invoker.addCommand(DropCommand(commits))
            modelService.branchInfo.clearSelectedCommits()
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
