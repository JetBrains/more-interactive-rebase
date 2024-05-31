package com.jetbrains.interactiveRebase.actions.gitPanel

import com.intellij.icons.AllIcons
import com.intellij.ide.HelpTooltip
import com.intellij.ide.IdeBundle
import com.intellij.ide.actions.GotoClassPresentationUpdater
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.ExperimentalUI
import com.jetbrains.interactiveRebase.services.ActionService
import java.awt.Dimension
import java.util.function.Supplier
import javax.swing.JComponent

class StopToEditAction :
    DumbAwareAction(
        "Stop to Edit",
        "Pauses the rebase action to edit a commit",
        AllIcons.Actions.Pause,
    ),
    CustomComponentAction {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<ActionService>()?.takeStopToEditAction()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        e.project?.service<ActionService>()?.checkStopToEdit(e)
    }

    override fun createCustomComponent(
            presentation: Presentation,
            place: String,
    ): JComponent {
        return RebaseActionsGroup.makeTooltip(this, presentation, place, "Alt+E",
                "Pauses the rebase action to edit a commit")

    }
}
