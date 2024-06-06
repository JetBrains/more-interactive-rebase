package com.jetbrains.interactiveRebase.actions.changePanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.jetbrains.interactiveRebase.services.ActionService

class UndoAction : AnAction("Undo", "Undo the last action", AllIcons.Actions.Undo) {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<ActionService>()?.undoLastAction()
    }

    override fun update(e: AnActionEvent) {
        e.project?.service<ActionService>()?.checkUndo(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}
