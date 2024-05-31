package com.jetbrains.interactiveRebase.actions.changePanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.jetbrains.interactiveRebase.services.ActionService

class AddBranchAction : AnAction("Add Branch", "Add another branch to the view", AllIcons.Actions.AddList) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val mainPanel = project.service<ActionService>().mainPanel
        val sidePanel = mainPanel.sidePanel

        sidePanel.isVisible = !sidePanel.isVisible
        sidePanel.setVisible(sidePanel.isVisible)

        // This toggles the icon of the add branch
        // e.presentation.icon = if (sidePanel.isVisible) AllIcons.Actions.Exit else AllIcons.Actions.AddList
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = true
//        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}
