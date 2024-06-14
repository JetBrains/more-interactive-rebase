package com.jetbrains.interactiveRebase.actions.changePanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.getActionShortcutText
import com.jetbrains.interactiveRebase.actions.gitPanel.RebaseActionsGroup
import com.jetbrains.interactiveRebase.visuals.GraphDiffDialog
import javax.swing.JComponent

class ViewDiffAction : DumbAwareAction(
    "See Difference",
    "See the difference with initial state",
    AllIcons.Actions.Diff,
),
    CustomComponentAction {
    override fun actionPerformed(e: AnActionEvent) {
        println("yaaa")
        val project = e.project
        if (null != project) {
            val dialog = GraphDiffDialog(project)
            dialog.show()


        }


    }

    override fun update(e: AnActionEvent) {}
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
            getActionShortcutText("com.jetbrains.interactiveRebase.actions.changePanel.ViewDiffAction"),
            "See the difference with initial state",
        )
    }

}