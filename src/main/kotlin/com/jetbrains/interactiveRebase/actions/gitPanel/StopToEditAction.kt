package com.jetbrains.interactiveRebase.actions.gitPanel

import com.intellij.icons.AllIcons
import com.intellij.ide.HelpTooltip
import com.intellij.ide.IdeBundle
import com.intellij.ide.actions.GotoClassPresentationUpdater
import com.intellij.openapi.actionSystem.*
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

class StopToEditAction : DumbAwareAction("Stop to Edit", "Pause the rebasing action to edit a commit", AllIcons.Actions.Pause), CustomComponentAction {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<ActionService>()?.takeStopToEditAction()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        e.project?.service<ActionService>()?.checkStopToEdit(e)
    }

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        return object : ActionButton(this, presentation, place, Supplier {getMinimumSize(place) }) {
            override fun updateToolTipText() {
                val classesTabName = java.lang.String.join("/", GotoClassPresentationUpdater.getActionTitlePluralized())
                if (Registry.`is`("ide.helptooltip.enabled")) {
                    HelpTooltip.dispose(this)
                    HelpTooltip()
                            .setTitle(myPresentation.text)
                            .setShortcut("Alt+E")
                            .setDescription("Stop while rebasing to edit a commit")
                            .installOn(this)
                } else {
                    toolTipText = IdeBundle.message("search.everywhere.action.tooltip.text", shortcutText, classesTabName)
                }
            }
        }
    }

    private fun getMinimumSize(place: String): Dimension {
        return if (isExperimentalToolbar(place)) ActionToolbar.experimentalToolbarMinimumButtonSize() else ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
    }

    private fun isExperimentalToolbar(place: String): Boolean {
        return ExperimentalUI.isNewUI() && ActionPlaces.MAIN_TOOLBAR == place
    }
}
