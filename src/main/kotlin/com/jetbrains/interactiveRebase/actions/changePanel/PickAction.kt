package com.jetbrains.interactiveRebase.actions.changePanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class PickAction : AnAction("Pick", "Undo the changes made", AllIcons.Actions.Undo) {
    override fun actionPerformed(e: AnActionEvent) {
        println("pick")
        TODO("Not yet implemented")
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = true
//        super.update(e)
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
}
