package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.commands.RebaseCommand

@Service(Service.Level.PROJECT)
class RebaseInvoker(project: Project) {
    /**
     * Global (project-level) list of rebase commands
     * that will be executed, once the rebase is initiated.
     */
    private var commands = mutableListOf<RebaseCommand>()

    /**
     * Adds a command to the list of commands to be executed.
     */
    fun addCommand(command: RebaseCommand) {
        commands.add(command)
    }

    /**
     * Removes a command from the list of commands to be executed.
     */
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
