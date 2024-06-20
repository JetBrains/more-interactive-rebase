package com.jetbrains.interactiveRebase.actions.gitPanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.getActionShortcutText
import com.jetbrains.interactiveRebase.services.ActionService
import javax.swing.JComponent

class StopToEditAction :
    DumbAwareAction(
        "Stop to Edit",
        "Pauses the rebase action to edit a commit",
        AllIcons.Actions.Pause,
    ),
    CustomComponentAction {
    override fun actionPerformed(e: AnActionEvent) {
        e.project!!.service<ActionService>().takeStopToEditAction()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        e.project!!.service<ActionService>().checkStopToEdit(e)
    }

    override fun createCustomComponent(
        presentation: Presentation,
        place: String,
    ): JComponent {
        return RebaseActionsGroup.makeTooltip(
            this,
            presentation,
            place,
            getActionShortcutText("com.jetbrains.interactiveRebase.actions.gitPanel.StopToEditAction"),
            "Pauses the rebase action to edit a commit",
        )
    }
}
