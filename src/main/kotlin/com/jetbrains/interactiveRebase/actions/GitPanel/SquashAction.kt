package com.jetbrains.interactiveRebase.actions.GitPanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service

class SquashAction() : AnAction("Squash", "Combine multiple commits into one", AllIcons.Actions.DynamicUsages) {
    override fun actionPerformed(e: AnActionEvent) {
        println("performed action")
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = true

        // TODO: consider which actions are available and disable accordingly see https://plugins.jetbrains.com/docs/intellij/basic-action-system.html#overriding-the-anactionupdate-method
        // can access services with with e.project.service<>()
        //
//        super.update(e)
    }
}