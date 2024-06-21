package com.jetbrains.interactiveRebase.actions.changePanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.getActionShortcutText
import com.jetbrains.interactiveRebase.actions.gitPanel.RebaseActionsGroup
import com.jetbrains.interactiveRebase.services.ActionService
import javax.swing.JComponent

class UndoAction :
    DumbAwareAction("Undo", "Undo the last action", AllIcons.Actions.Undo),
    CustomComponentAction {
    override fun actionPerformed(e: AnActionEvent) {
        e.project!!.service<ActionService>().undoLastAction()
    }

    override fun update(e: AnActionEvent) {
        e.project!!.service<ActionService>().checkIfChangesMade(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun createCustomComponent(
        presentation: Presentation,
        place: String,
    ): JComponent {
        return RebaseActionsGroup.makeTooltip(
            this,
            presentation,
            place,
            getActionShortcutText("com.jetbrains.interactiveRebase.actions.changePanel.UndoAction"),
            "Undo the last action",
        )
    }
}
