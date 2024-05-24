package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand

@Service(Service.Level.PROJECT)
class ActionService(private val project: Project) {
    private var modelService = project.service<ModelService>()
    private var invoker = project.service<RebaseInvoker>()

    /**
     * Constructor for injection during testing
     */
    constructor(project: Project, modelService: ModelService, invoker: RebaseInvoker) : this(project) {
        this.modelService = modelService
        this.invoker = invoker
    }

    fun takeRewordAction() {
        modelService.branchInfo.selectedCommits.forEach {
            it.setDoubleClickedTo(true)
        }
    }

    fun takeDropAction() {
        val commits = modelService.getSelectedCommits()
        commits.forEach {
                commitInfo ->
            commitInfo.addChange(DropCommand(mutableListOf(commitInfo)))
        }
        invoker.addCommand(DropCommand(commits))
        modelService.branchInfo.clearSelectedCommits()
    }

    fun checkDrop(e: AnActionEvent) {
        //        e.presentation.isEnabledAndVisible = true
        if (modelService.branchInfo.selectedCommits.size < 1) {
            e.presentation.isEnabled = false
        }
    }

    fun checkReword(e: AnActionEvent) {
        e.presentation.isEnabled = true
        e.presentation.isEnabledAndVisible = true
        val project = e.project
        if (project != null && project.service<ModelService>().branchInfo.selectedCommits.size != 1) {
            e.presentation.isEnabled = false
        }
    }
}
