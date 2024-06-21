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

class RedoAction :
    DumbAwareAction("Redo", "Redo the last action", AllIcons.Actions.Redo),
    CustomComponentAction {
    override fun actionPerformed(e: AnActionEvent) {
        e.project!!.service<ActionService>().redoLastAction()
    }

    override fun update(e: AnActionEvent) {
        e.project!!.service<ActionService>().checkRedo(e)
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
            getActionShortcutText("com.jetbrains.interactiveRebase.actions.changePanel.RedoAction"),
            "Redo the last action",
        )
    }
}
