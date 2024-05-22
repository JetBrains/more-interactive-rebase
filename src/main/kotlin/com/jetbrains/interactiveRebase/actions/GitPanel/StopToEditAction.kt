package com.jetbrains.interactiveRebase.actions.GitPanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class StopToEditAction : AnAction("Stop to Edit", "Pauses the rebasing action to edit a commit", AllIcons.Actions.Pause) {
    override fun actionPerformed(e: AnActionEvent) {
        println("stop to edit")
        TODO("Not yet implemented")
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = true
//        super.update(e)
    }

}