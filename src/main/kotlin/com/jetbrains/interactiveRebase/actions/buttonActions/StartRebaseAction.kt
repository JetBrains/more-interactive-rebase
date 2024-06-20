package com.jetbrains.interactiveRebase.actions.buttonActions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.jetbrains.interactiveRebase.dataClasses.commands.CherryCommand
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.RebaseInvoker

class StartRebaseAction :
    ButtonAction(
        "Rebase",
        "Start the rebase process with the indicated changes",
        "StartRebaseAction",
    ) {
    override fun actionPerformed(e: AnActionEvent) {
        val invoker = e.project?.service<RebaseInvoker>()
        if(invoker?.commands?.filterIsInstance<CherryCommand>()?.size==0){
            invoker.executeCommands()
        }else{
            invoker?.executeCherry()
        }

    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        val project = e.project ?: return
        val actionService = project.service<ActionService>()
        actionService.checkRebaseAndReset(e)
    }
}
