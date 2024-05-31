package com.jetbrains.interactiveRebase.actions.gitPanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.interactiveRebase.services.ActionService
import javax.swing.JComponent

class FixupAction :
    DumbAwareAction(
        "Fixup",
        "Combines commits into one, with a default message",
        AllIcons.Actions.ListFiles,
    ),
    CustomComponentAction {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<ActionService>()?.takeFixupAction()
    }

    override fun update(e: AnActionEvent) {
        e.project?.service<ActionService>()?.checkFixupOrSquash(e)
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
            "Alt+F",
            "Combines commits into one, with a default message",
        )
    }
}
