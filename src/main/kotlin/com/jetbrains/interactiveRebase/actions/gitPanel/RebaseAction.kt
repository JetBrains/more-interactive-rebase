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
        "Rebase Onto",
        "Change the base of your checked-out branch.",
        AllIcons.Vcs.Branch,
    ),
    CustomComponentAction {
    override fun actionPerformed(e: AnActionEvent) {
        val mainPanel = e.project!!.service<ActionService>().mainPanel
        val graphPanel = mainPanel.graphPanel
        var base = e.project!!.service<ModelService>().graphInfo.addedBranch?.currentCommits!![0]
        val rebaseDragAndDropListener =
            RebaseDragAndDropListener(
                e.project!!,
                graphPanel.mainBranchPanel.branchNamePanel,
                graphPanel.addedBranchPanel!!.branchNamePanel,
                graphPanel,
            )
        if (e.project!!.service<ModelService>().graphInfo.addedBranch?.selectedCommits!!.isNotEmpty()) {
            base = e.project!!.service<ModelService>().graphInfo.addedBranch?.selectedCommits!![0]
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
            getActionShortcutText("com.jetbrains.interactiveRebase.actions.gitPanel.RebaseAction"),
            "Change the base of your checked-out branch.",
        )
    }
}
