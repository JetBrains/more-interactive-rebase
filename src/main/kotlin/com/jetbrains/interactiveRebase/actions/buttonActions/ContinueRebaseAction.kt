package com.jetbrains.interactiveRebase.actions.buttonActions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.getActionShortcutText
import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.actions.gitPanel.RebaseActionsGroup
import git4idea.actions.GitRebaseContinue
import javax.swing.JComponent

class ContinueRebaseAction : GitRebaseContinue(), CustomComponentAction {
    override fun update(e: AnActionEvent) {
        e.presentation.icon = getMainToolbarIcon()
    }

    override fun performActionForProject(
        project: Project,
        indicator: ProgressIndicator,
    ) {
        super.performActionForProject(project, indicator)
    }

    override fun createCustomComponent(
        presentation: Presentation,
        place: String,
    ): JComponent {
        val button =
            RebaseActionsGroup.makeTooltip(
                this,
                presentation,
                place,
                getActionShortcutText("com.jetbrains.interactiveRebase.actions.buttonActions.ContinueRebaseAction"),
                "Continue the rebase process",
            )
        button.putClientProperty("JButton.backgroundColor", JBColor.GREEN)
        return button
    }
}
