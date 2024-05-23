package com.jetbrains.interactiveRebase.actions.gitPanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.jetbrains.interactiveRebase.services.ModelService

class RewordAction : DumbAwareAction("Reword", "Change the subject of a commit", AllIcons.Actions.SuggestedRefactoringBulb) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        if (project != null) {
            val modelService = project.service<ModelService>()
            modelService.branchInfo.selectedCommits.forEach {
                it.setDoubleClickedTo(true)
            }
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = true
        e.presentation.isEnabledAndVisible = true
        val project = e.project
        if (project != null && project.service<ModelService>().branchInfo.selectedCommits.size != 1) {
            e.presentation.isEnabled = false
        }

//        val component = e.presentation.getClientProperty(CustomComponentAction.COMPONENT_KEY)
//        if (component is JButton) {
//            component.setToolTipText("This is the help text for the button")
//        }
//        super.update(e)
    }
}
