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

class AddBranchAction :
    DumbAwareAction(
        "Add Branch",
        "Add another branch to the view",
        AllIcons.Actions.AddList,
    ),
    CustomComponentAction {
    override fun actionPerformed(e: AnActionEvent) {
        val mainPanel = e.project!!.service<ActionService>().mainPanel
        val sidePanelPane = mainPanel.sidePanelPane

        sidePanelPane.isVisible = !sidePanelPane.isVisible
        if (sidePanelPane.isVisible) {
            mainPanel.sidePanel.updateBranchNames()
        }

        sidePanelPane.setVisible(sidePanelPane.isVisible)

        // This toggles the icon of the add branch
        e.presentation.icon = if (sidePanelPane.isVisible) AllIcons.Actions.ListFiles else AllIcons.Actions.AddList
    }

    override fun update(e: AnActionEvent) {
        val project = e.project!!
        e.presentation.isEnabled = project.service<ActionService>().checkRebaseIsNotInProgress()
        e.presentation.isVisible = true
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
            getActionShortcutText("com.jetbrains.interactiveRebase.actions.changePanel.AddBranchAction"),
            "Add another branch to the view",
        )
    }
}
