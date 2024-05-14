package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.commands.RebaseCommand

@Service(Service.Level.PROJECT)
class RebaseInvoker(project: Project) {
    private var commands = mutableListOf<RebaseCommand>()

    fun addCommand(command: RebaseCommand) {
        commands.add(command)
    }

    fun removeCommand(command: RebaseCommand) {
        commands.remove(command)
    }

    /**
     * Executes all the commands to be able to perform the rebase.
     */
    fun executeCommands() {
        commands.forEach { it.execute() }
    }
}