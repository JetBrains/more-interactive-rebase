package com.jetbrains.interactiveRebase.actions.GitPanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class RewordAction : DumbAwareAction("Reword", "Reword a commit", AllIcons.Actions.SuggestedRefactoringBulb) {
    override fun actionPerformed(e: AnActionEvent) {
        println("run to cursor")
//        TODO("Not yet implemented")
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
//        println("updated")
        e.presentation.isEnabled = true
        e.presentation.isEnabledAndVisible = true
//        e.presentation.
//        super.update(e)
    }
}