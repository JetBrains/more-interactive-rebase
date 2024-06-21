package com.jetbrains.interactiveRebase.actions.gitPanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.getActionShortcutText
import com.jetbrains.interactiveRebase.actions.gitPanel.RebaseActionsGroup.Companion.makeTooltip
import com.jetbrains.interactiveRebase.services.ActionService
import javax.swing.JComponent

class RewordAction :
    DumbAwareAction(
        "Reword",
        "Changes the subject of a commit",
        AllIcons.Actions.SuggestedRefactoringBulb,
    ),
    CustomComponentAction {
    override fun actionPerformed(e: AnActionEvent) {
        e.project!!.service<ActionService>().takeRewordAction()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        e.project!!.service<ActionService>().checkReword(e)
    }

    override fun createCustomComponent(
        presentation: Presentation,
        place: String,
    ): JComponent {
        return makeTooltip(
            this,
            presentation,
            place,
            getActionShortcutText("com.jetbrains.interactiveRebase.actions.gitPanel.RewordAction"),
            "Changes the subject of a commit",
        )
    }
}
