package com.jetbrains.interactiveRebase.actions.gitPanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.interactiveRebase.services.ActionService

class SquashAction() : DumbAwareAction("Squash", "Combine multiple commits into one", AllIcons.Actions.DynamicUsages) {
    override fun actionPerformed(e: AnActionEvent) {
        println("performed action")
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        e.project?.service<ActionService>()?.checkDrop(e) // TODO replace with actual implementation
    }
}
