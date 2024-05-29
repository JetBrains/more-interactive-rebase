package com.jetbrains.interactiveRebase.actions.gitPanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.jetbrains.interactiveRebase.services.ActionService

class PickAction : AnAction("Pick", "Undo the changes made", AllIcons.Diff.GutterCheckBoxSelected) {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<ActionService>()?.performPickAction()
    }

    override fun update(e: AnActionEvent) {
        e.project?.service<ActionService>()?.checkPick(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}
