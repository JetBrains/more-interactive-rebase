package com.jetbrains.interactiveRebase.actions.gitPanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.interactiveRebase.services.ActionService

class StopToEditAction : DumbAwareAction("Stop to Edit", "Pause the rebasing action to edit a commit", AllIcons.Actions.Pause) {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<ActionService>()?.takeStopToEditAction()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        e.project?.service<ActionService>()?.checkStopToEdit(e)
    }
}
