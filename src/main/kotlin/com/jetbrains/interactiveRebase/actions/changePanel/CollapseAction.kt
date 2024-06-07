package com.jetbrains.interactiveRebase.actions.changePanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.interactiveRebase.services.ActionService

class CollapseAction: DumbAwareAction("Collapse", "Collapse commits", AllIcons.General.CollapseComponent) {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<ActionService>()?.takeCollapseAction()
    }

    override fun update(e: AnActionEvent) {
        e.project?.service<ActionService>()?.checkCollapse(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}