package com.jetbrains.interactiveRebase.actions.gitPanel

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction

import com.jetbrains.interactiveRebase.services.ActionService

import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.StopToEditCommand
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.services.RebaseInvoker


class SquashAction() : DumbAwareAction("Squash", "Combine multiple commits into one", AllIcons.Actions.DynamicUsages) {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<ActionService>()?.takeSquashAction()

    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        e.project?.service<ActionService>()?.checkDrop(e) // TODO replace with actual implementation
    }
}
