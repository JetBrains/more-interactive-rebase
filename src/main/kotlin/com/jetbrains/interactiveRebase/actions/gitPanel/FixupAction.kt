package com.jetbrains.interactiveRebase.actions.gitPanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.interactiveRebase.services.ActionService

class FixupAction : DumbAwareAction("Fixup", "Combine commits and set a default message", AllIcons.Actions.ListFiles) {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<ActionService>()?.takeFixupAction()
    }

    override fun update(e: AnActionEvent) {
        e.project?.service<ActionService>()?.checkDrop(e) // TODO replace with actual implementation
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }


}
