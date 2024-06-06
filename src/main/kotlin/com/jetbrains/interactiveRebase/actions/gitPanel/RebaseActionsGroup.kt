package com.jetbrains.interactiveRebase.actions.gitPanel

import com.intellij.ide.HelpTooltip
import com.intellij.ide.IdeBundle
import com.intellij.ide.actions.GotoClassPresentationUpdater
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.util.registry.Registry
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
        internal fun makeTooltip(
            action: AnAction,
            presentation: Presentation,
            place: String,
            shortcut: String,
            description: String,
        ): JComponent {
            return object : ActionButton(
                action,
                presentation,
                place,
                Supplier {
                    ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
                },
            ) {
                override fun updateToolTipText() {
                    val classesTabName =
                        java.lang.String.join(
                            "/",
                            GotoClassPresentationUpdater.getActionTitlePluralized(),
                        )
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
