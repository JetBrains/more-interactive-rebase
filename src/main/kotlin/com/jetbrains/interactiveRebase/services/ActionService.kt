package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand

@Service(Service.Level.PROJECT)
class ActionService(project: Project) {
    private var modelService = project.service<ModelService>()
    private var invoker = project.service<RebaseInvoker>()

    /**
     * Constructor for injection during testing
     */
    constructor(project: Project, modelService: ModelService, invoker: RebaseInvoker) : this(project) {
        this.modelService = modelService
        this.invoker = invoker
    }

    /**
     * Enables the text field once the Reword button on the toolbar is pressed
     */
    fun takeRewordAction() {
        modelService.branchInfo.selectedCommits.forEach {
            it.setDoubleClickedTo(true)
        }
    }

    /**
     * Makes a drop change once the Drop button is clicked
     */
    fun takeDropAction() {
        val commits = modelService.getSelectedCommits()
        commits.forEach {
                commitInfo ->
            val command = DropCommand(mutableListOf(commitInfo))
            commitInfo.addChange(command)
            invoker.addCommand(command)
        }

        modelService.branchInfo.clearSelectedCommits()
    }

    /**
     * Enables the Drop button if there are selected commits
     */
    fun checkDrop(e: AnActionEvent) {
        e.presentation.isEnabled = modelService.branchInfo.selectedCommits.isNotEmpty()
    }

    /**
     * Enables reword button if one commit is selected
     */
    fun checkReword(e: AnActionEvent) {
        e.presentation.isEnabled = modelService.branchInfo.selectedCommits.size == 1
    }
}
