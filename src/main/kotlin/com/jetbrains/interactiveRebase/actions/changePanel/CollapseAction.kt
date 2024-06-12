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

class CollapseAction : DumbAwareAction("Collapse", "Collapse commits",
        AllIcons.General.CollapseComponent), CustomComponentAction {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<ActionService>()?.takeCollapseAction()
    }

    override fun update(e: AnActionEvent) {
        e.project?.service<ActionService>()?.checkCollapse(e)
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
                getActionShortcutText("com.jetbrains.interactiveRebase.actions.changePanel.CollapseAction"),
                "Collapse commits",
        )
    }
}
