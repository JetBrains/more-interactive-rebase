package com.jetbrains.interactiveRebase.actions.buttonActions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.jetbrains.interactiveRebase.services.ActionService

public class ResetAction :
    ButtonAction("Reset", "Reset all changes", "ResetAction") {
    override fun actionPerformed(e: AnActionEvent) {
        val actionService = e.project?.service<ActionService>()
        actionService?.resetAllChangesAction()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        val actionService = project.service<ActionService>()
        actionService.checkRebaseAndReset(e)
    }
}
