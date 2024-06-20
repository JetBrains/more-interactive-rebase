package com.jetbrains.interactiveRebase.actions.buttonActions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.getActionShortcutText
import com.jetbrains.interactiveRebase.actions.gitPanel.RebaseActionsGroup
import com.jetbrains.interactiveRebase.services.ModelService
import git4idea.actions.GitRebaseAbort
import javax.swing.JComponent

class AbortRebaseAction : GitRebaseAbort(), CustomComponentAction {
    override fun update(e: AnActionEvent) {
        e.presentation.icon = getMainToolbarIcon()
    }

    override fun performActionForProject(
        project: Project,
        indicator: ProgressIndicator,
    ) {
        super.performActionForProject(project, indicator)
        project.service<ModelService>().refreshModel()
    }

    override fun createCustomComponent(
        presentation: Presentation,
        place: String,
    ): JComponent {
        return RebaseActionsGroup.makeTooltip(
            this,
            presentation,
            place,
            getActionShortcutText("com.jetbrains.interactiveRebase.actions.buttonActions.AbortRebaseAction"),
            "Abort the rebase process",
        )
    }
}
