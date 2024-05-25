package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.FixupCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.ReorderCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.StopToEditCommand

@Service(Service.Level.PROJECT)
class ActionService(project: Project) {
    private var modelService = project.service<ModelService>()
    private var invoker = modelService.invoker

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
            val command = DropCommand(commitInfo)
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

    /**
     * Adds a visual change for a commit that has to be stopped to edit
     */
    fun takeStopToEditAction() {
        val commits = modelService.getSelectedCommits()
        commits.forEach {
                commitInfo ->
            val command = StopToEditCommand(commitInfo)
            commitInfo.addChange(command)
            invoker.addCommand(command)
        }
        modelService.branchInfo.clearSelectedCommits()
    }

    /**
     * Picks the selected commits and removes all changes except for reorders,
     * similar to the logic in the git to-do file for rebasing
     */
    fun performPickAction() {
        val commits = modelService.getSelectedCommits()
        commits.forEach { commitInfo ->
            val changes = commitInfo.changes.iterator()

            while (changes.hasNext()) {
                val change = changes.next()
                if (change !is ReorderCommand) {
                    invoker.removeCommand(change)
                    changes.remove()
                }
            }
        }
        modelService.branchInfo.clearSelectedCommits()
    }

    /**
     * Adds a visual change for a commit that has to be squashed hardcoded
     */
    fun takeSquashAction() {
        if (invoker != null) {
            invoker.addCommand(
                SquashCommand(
                    invoker.branchInfo.selectedCommits.last(),
                    invoker.branchInfo.selectedCommits.subList(0, invoker.branchInfo.selectedCommits.size - 1),
                    "s",
                ),
            )
        }
    }

    /**
     * Adds a visual change for a commit that has to be fixed up hardcoded
     */
    fun takeFixupAction() {
        if (invoker != null) {
            invoker.addCommand(
                FixupCommand(
                    invoker.branchInfo.selectedCommits.last(),
                    invoker.branchInfo.selectedCommits.subList(0, invoker.branchInfo.selectedCommits.size - 1),
                ),
            )
        }
    }

    /**
     * Resets all changes made to the commits
     */
    fun resetAllChangesAction() {
        invoker.commands = mutableListOf()
        val currentBranchInfo = invoker.branchInfo
        invoker.branchInfo.currentCommits = currentBranchInfo.initialCommits.toMutableList()
        invoker.branchInfo.initialCommits.forEach {
                commitInfo ->
            commitInfo.changes.clear()
        }
        invoker.branchInfo.clearSelectedCommits()
    }

    fun takeSquashAction() {
        takeFixupAction()
        takeRewordAction()
    }

    fun takeFixupAction() {
        val commits: MutableList<CommitInfo> = modelService.getSelectedCommits()
        val parent = commits.last()
        // get the parent one with diegos logic
        val command = SquashCommand(mutableListOf(parent), commits.dropLast(1), "new message")
        invoker.addCommand(command)

        println("selected commits are $commits")

        commits.forEach {
                commitInfo ->
            commitInfo.isSelected = false
            commitInfo.isHovered = false

            if (commits.last() != commitInfo) {
                modelService.branchInfo.currentCommits.remove(commitInfo)
                println("remove $commitInfo")
            }
            commitInfo.addChange(command)
        }
        modelService.branchInfo.clearSelectedCommits()
        modelService.branchInfo.addSelectedCommits(parent)
        println("exitiing fixup selected are ${modelService.branchInfo.selectedCommits}")
        println("exit fixup current are ${modelService.branchInfo.currentCommits}")
        // TODO add to the model for backend
    }
}
