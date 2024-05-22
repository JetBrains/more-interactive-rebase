package com.jetbrains.interactiveRebase.actions.GitPanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class FixupAction : AnAction("Fixup", "Combines commits and sets a default message", AllIcons.Actions.ListFiles) {


    override fun actionPerformed(e: AnActionEvent) {
        println("fixup")
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