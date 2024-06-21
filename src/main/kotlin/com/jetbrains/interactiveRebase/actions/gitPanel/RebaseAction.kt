package com.jetbrains.interactiveRebase.actions.gitPanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.getActionShortcutText
import com.jetbrains.interactiveRebase.listeners.RebaseDragAndDropListener
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.ModelService
import javax.swing.JComponent

class RebaseAction :
    DumbAwareAction(
        "Rebase onto",
        "Change the base of your checked-out branch.",
        AllIcons.Vcs.Branch,
    ),
    CustomComponentAction {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project!!
        val graphPanel = project.service<ActionService>().mainPanel.graphPanel
        val rebaseDragAndDropListener =
            RebaseDragAndDropListener(
                project,
                graphPanel.mainBranchPanel.branchNamePanel,
                graphPanel.addedBranchPanel!!.branchNamePanel,
                graphPanel,
            )
        var base = project.service<ModelService>().graphInfo.addedBranch?.currentCommits!![0]
        if (project.service<ModelService>().graphInfo.addedBranch?.selectedCommits!!.isNotEmpty()) {
            base = project.service<ModelService>().graphInfo.addedBranch?.selectedCommits!![0]
        }
        rebaseDragAndDropListener.rebase(base)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        e.project!!.service<ActionService>().checkNormalRebaseAction(e)
    }

    override fun createCustomComponent(
        presentation: Presentation,
        place: String,
    ): JComponent {
        return RebaseActionsGroup.makeTooltip(
            this,
            presentation,
            place,
            getActionShortcutText("com.jetbrains.interactiveRebase.actions.gitPanel.DropAction"),
            "Change the base of your checked-out branch.",
        )
    }
}
