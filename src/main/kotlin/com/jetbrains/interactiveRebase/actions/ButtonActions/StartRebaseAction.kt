package com.jetbrains.interactiveRebase.actions.ButtonActions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.RebaseInvoker


internal class StartRebaseAction :
        ButtonAction("Rebase",
                "Start the rebase process with the indicated changes",
                "StartRebaseAction") {

    override fun actionPerformed(e: AnActionEvent) {
        val invoker = e.project?.service<RebaseInvoker>()
        invoker?.createModel()
        invoker?.executeCommands()

    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        e.project?.service<ActionService>()?.checkRebaseAndReset(e)

    }


}