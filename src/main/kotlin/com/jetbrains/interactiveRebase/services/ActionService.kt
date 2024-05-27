package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.FixupCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.ReorderCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.StopToEditCommand
import com.jetbrains.interactiveRebase.exceptions.IRInaccessibleException
import com.jetbrains.rd.framework.base.deepClonePolymorphic

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
        e.presentation.isEnabled = modelService.branchInfo.getActualSelectedCommitsSize() == 1
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
     * Resets all changes made to the commits
     */
    fun resetAllChangesAction() {
        invoker.commands = mutableListOf()
        val currentBranchInfo = invoker.branchInfo
        invoker.branchInfo.currentCommits = currentBranchInfo.initialCommits.toMutableList()
        invoker.branchInfo.initialCommits.forEach {
                commitInfo ->
            commitInfo.changes.clear()
            commitInfo.isSelected = false
            commitInfo.isSquashed = false
            commitInfo.isDoubleClicked = false
            commitInfo.isDragged = false
            commitInfo.isReordered = false
            commitInfo.isHovered = false
        }
        invoker.branchInfo.clearSelectedCommits()
    }

    fun takeSquashAction() {
        takeFixupAction()
        takeRewordAction()
    }

    fun takeFixupAction() {
        val selectedCommits: MutableList<CommitInfo> = modelService.getSelectedCommits()
        selectedCommits.sortBy { modelService.branchInfo.currentCommits.indexOf(it) }
        var parentCommit = selectedCommits.last()
        if (selectedCommits.size == 1) {
            val selectedIndex = modelService.getCurrentCommits().indexOf(selectedCommits[0])

            if (selectedIndex == modelService.getCurrentCommits().size - 1) {
                throw IRInaccessibleException("Commit can not be squashed (parent commit not found)")
            }

            parentCommit = modelService.getCurrentCommits()[selectedIndex + 1]
        }
        selectedCommits.remove(parentCommit)
        val fixupCommits = selectedCommits.deepClonePolymorphic()
        val command = FixupCommand(parentCommit, fixupCommits)

        fixupCommits.forEach {
                commit ->
            commit.isSelected = false
            commit.isHovered = false
            commit.isSquashed = true
            modelService.branchInfo.currentCommits.remove(commit)

            commit.addChange(command)
        }

        parentCommit.addChange(command)
        modelService.branchInfo.clearSelectedCommits()

        invoker.addCommand(command)
        // TODO add to the model for backend
    }
}
