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

class PickAction :
    DumbAwareAction(
        "Pick",
        "Undoes the changes made on the selected commit",
        AllIcons.Diff.GutterCheckBoxSelected,
    ),
    CustomComponentAction {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<ActionService>()?.performPickAction()
    }

    override fun update(e: AnActionEvent) {
        e.project?.service<ActionService>()?.checkPick(e)
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
            "Alt+P",
            "Undoes the changes made on the selected commit",
        )
    }
}
