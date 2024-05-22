package com.jetbrains.interactiveRebase.actions.GitPanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.services.ComponentService

class DropAction: AnAction("Drop", "Drops a commit", AllIcons.Actions.DeleteTagHover) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        if (project != null) {
            val service = project.service<ComponentService>()
            service.getSelectedCommits().forEach {
                    commitInfo ->
                commitInfo.changes.add(DropCommand(commitInfo))

                commitInfo.isSelected = false
                service.branchInfo.selectedCommits.remove(commitInfo)
            }
        }

    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = true
//        super.update(e)
    }
}