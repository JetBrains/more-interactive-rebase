package com.jetbrains.interactiveRebase.actions.gitPanel

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.getActionShortcutText
import com.jetbrains.interactiveRebase.services.ActionService
import icons.DvcsImplIcons
import javax.swing.JComponent

class CherryAction :
    DumbAwareAction("Cherry Pick", "Removes a commit from history", DvcsImplIcons.CherryPick),
    CustomComponentAction {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<ActionService>()?.takeCherryPickAction()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        e.project?.service<ActionService>()?.checkCherryPick(e)
    }

    override fun createCustomComponent(
        presentation: Presentation,
        place: String,
    ): JComponent {
        return RebaseActionsGroup.makeTooltip(
            this,
            presentation,
            place,
            getActionShortcutText("com.jetbrains.interactiveRebase.actions.gitPanel.CherryAction"),
            "Removes a commit from history",
        )
    }
}
