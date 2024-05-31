package com.jetbrains.interactiveRebase.actions.gitPanel

import com.intellij.ide.HelpTooltip
import com.intellij.ide.IdeBundle
import com.intellij.ide.actions.GotoClassPresentationUpdater
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.ExperimentalUI
import java.awt.Dimension
import java.util.function.Supplier
import javax.swing.JComponent

class RebaseActionsGroup : DefaultActionGroup() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(event: AnActionEvent) {
        event.presentation.setEnabled(true)
    }
    companion object {
        internal fun getMinimumSize(place: String) : Dimension {
            return if (isExperimentalToolbar(place)) {
                ActionToolbar.experimentalToolbarMinimumButtonSize()
            } else {
                ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
            }

        }

        private fun isExperimentalToolbar(place: String): Boolean {
            return ExperimentalUI.isNewUI() && ActionPlaces.MAIN_TOOLBAR == place
        }

        internal fun makeTooltip( action: AnAction,
                         presentation: Presentation,
                         place: String,
                         shortcut: String,
                         description: String): JComponent {
            return object : ActionButton(action, presentation, place, Supplier { getMinimumSize(place) }) {
                override fun updateToolTipText() {
                    val classesTabName = java.lang.String.join("/", GotoClassPresentationUpdater.getActionTitlePluralized())
                    if (Registry.`is`("ide.helptooltip.enabled")) {
                        HelpTooltip.dispose(this)
                        HelpTooltip()
                                .setTitle(myPresentation.text)
                                .setShortcut(shortcut)
                                .setDescription(description)
                                .installOn(this)
                    } else {
                        toolTipText =
                                IdeBundle.message(
                                        "search.everywhere.action.tooltip.text",
                                        shortcutText,
                                        classesTabName,
                                )
                    }
                }
            }

        }


    }
}
