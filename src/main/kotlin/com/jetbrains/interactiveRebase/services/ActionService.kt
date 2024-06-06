package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.OnePixelSplitter
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.FixupCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.RebaseCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.ReorderCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.StopToEditCommand
import com.jetbrains.interactiveRebase.visuals.HeaderPanel
import com.jetbrains.interactiveRebase.visuals.MainPanel

@Service(Service.Level.PROJECT)
class ActionService(project: Project) {
    private var modelService = project.service<ModelService>()
    private var invoker = modelService.invoker
    internal lateinit var mainPanel: MainPanel

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
            it.setTextFieldEnabledTo(true)
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
     * Enables the Drop button
     */
    fun checkDrop(e: AnActionEvent) {
        e.presentation.isEnabled = modelService.branchInfo.selectedCommits.isNotEmpty() &&
            modelService.getSelectedCommits().none { commit ->
                commit.changes.any { change -> change is DropCommand }
            }
    }

    /**
     * Enables reword button
     */
    fun checkReword(e: AnActionEvent) {
        e.presentation.isEnabled = modelService.branchInfo.getActualSelectedCommitsSize() == 1 &&
            modelService.getSelectedCommits().none { commit ->
                commit.changes.any { change -> change is DropCommand }
            }
    }

    /**
     * Enables stop-to-edit button
     */
    fun checkStopToEdit(e: AnActionEvent) {
        e.presentation.isEnabled = modelService.branchInfo.selectedCommits.isNotEmpty() &&
            modelService.getSelectedCommits().none { commit ->
                commit.changes.any { change -> change is DropCommand }
            }
    }

    /**
     * Enables fixup or squash button
     */
    fun checkFixupOrSquash(e: AnActionEvent) {
        e.presentation.isEnabled = modelService.branchInfo.selectedCommits.isNotEmpty() &&
            !(
                modelService.branchInfo.selectedCommits.size==1 &&
                    invoker.branchInfo.currentCommits.reversed()
                        .indexOf(modelService.branchInfo.selectedCommits[0])==0
            ) &&
            modelService.getSelectedCommits().none { commit ->
                commit.changes.any { change -> change is DropCommand }
            }
    }

    /**
     * Enables pick button
     */
    fun checkPick(e: AnActionEvent) {
        e.presentation.isEnabled = modelService.branchInfo.selectedCommits.isNotEmpty()
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
                if (change is FixupCommand && change.parentCommit == commitInfo) {
                    clearFixupOnPick(change, commitInfo)
                }
                if (change is SquashCommand && change.parentCommit == commitInfo) {
                    clearSquashOnPick(change, commitInfo)
                }
                if (change !is ReorderCommand) {
                    invoker.removeCommand(change)
                    changes.remove()
                }
            }
        }
        modelService.branchInfo.clearSelectedCommits()
    }

    /**
     * Puts back the fixup commits to the list of current commits
     */
    fun clearFixupOnPick(
        change: FixupCommand,
        commitInfo: CommitInfo,
    ) {
        change.fixupCommits.forEach {
                fixupCommit ->
            fixupCommit.isSquashed = false
            fixupCommit.changes.clear()
        }
        val parentCommit = change.parentCommit
        val parentIndex = modelService.getCurrentCommits().indexOfFirst { it == parentCommit }
        if (parentIndex != -1) {
            modelService.getCurrentCommits().addAll(parentIndex, change.fixupCommits)
        }
        commitInfo.isSquashed = false
    }

    /**
     * Puts back the squashed commits to the list of current commits
     */
    fun clearSquashOnPick(
        change: SquashCommand,
        commitInfo: CommitInfo,
    ) {
        change.squashedCommits.forEach {
                squashedCommit ->
            squashedCommit.isSquashed = false
            squashedCommit.changes.clear()
        }
        val parentCommit = change.parentCommit
        val parentIndex = modelService.getCurrentCommits().indexOfFirst { it == parentCommit }
        if (parentIndex != -1) {
            modelService.getCurrentCommits().addAll(parentIndex, change.squashedCommits)
        }
        commitInfo.isSquashed = false
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
            commitInfo.isTextFieldEnabled = false
            commitInfo.isDragged = false
            commitInfo.isReordered = false
            commitInfo.isHovered = false
        }
        invoker.branchInfo.clearSelectedCommits()
    }

    /**
     * Takes squash action and
     * creates a squash command
     */
    fun takeSquashAction() {
        combineCommits(true)
    }

    /**
     * Takes fixup action and
     * creates a fixup command
     */
    fun takeFixupAction() {
        combineCommits(false)
    }

    /**
     * Used for squash or fixup,
     * combines commits that are involved and turns them into their corresponding commands
     */
    private fun combineCommits(isSquash: Boolean) {
        val selectedCommits: MutableList<CommitInfo> = modelService.getSelectedCommits()
        selectedCommits.sortBy { modelService.branchInfo.indexOfCommit(it) }
        var parentCommit = selectedCommits.last()
        if (modelService.branchInfo.getActualSelectedCommitsSize() == 1) {
            val selectedIndex = modelService.getCurrentCommits().indexOf(selectedCommits[0])
            parentCommit = modelService.getCurrentCommits()[selectedIndex + 1]
        }
        selectedCommits.remove(parentCommit)
        val fixupCommits = cleanSelectedCommits(parentCommit, selectedCommits)
        var command: RebaseCommand = FixupCommand(parentCommit, fixupCommits)

        if (isSquash) {
            command = SquashCommand(parentCommit, fixupCommits, parentCommit.commit.subject)
            parentCommit.setTextFieldEnabledTo(true)
        }

        fixupCommits.forEach {
                commit ->
            handleCombinedCommits(commit, command)
        }

        parentCommit.addChange(command)
        parentCommit.isSelected = false
        modelService.branchInfo.clearSelectedCommits()

        invoker.addCommand(command)
    }

    /**
     * For fixup and squashed commits, handles the flags for commits involved that are not the parent
     */
    private fun handleCombinedCommits(
        commit: CommitInfo,
        command: RebaseCommand,
    ) {
        commit.isSelected = false
        commit.isHovered = false
        commit.isSquashed = true
        modelService.branchInfo.currentCommits.remove(commit)

        commit.addChange(command)
    }

    /**
     * Removes squashing/fixup change
     * from all commits and returns list
     * of commits to squash
     */
    private fun cleanSelectedCommits(
        parent: CommitInfo,
        selectedCommits: List<CommitInfo>,
    ): MutableList<CommitInfo> {
        val ret = mutableListOf<CommitInfo>()
        selectedCommits.forEach {
            removeSquashFixChange(it)
            ret.add(it)
        }

        removeSquashFixChange(parent)

        return ret
    }

    /**
     * Removes squash and fixup
     * commands from commitInfo
     * and the invoker
     */
    private fun removeSquashFixChange(commit: CommitInfo) {
        val changesToRemove = mutableListOf<RebaseCommand>()
        commit.changes.forEach {
            if (it is FixupCommand || it is SquashCommand) {
                modelService.invoker.removeCommand(it)
                changesToRemove.add(it)
            }
        }
        commit.changes.removeAll(changesToRemove)
    }

    /**
     * Called to either get the fixupCommits or squashedCommits parameter of a command,
     * to be used when fixup and squashed command is being used interchangeably
     */
    fun getCombinedCommits(change: RebaseCommand): List<CommitInfo> {
        return when (change) {
            is FixupCommand -> change.fixupCommits
            is SquashCommand -> change.squashedCommits
            else -> emptyList()
        }
    }

    fun getHeaderPanel(): HeaderPanel {
        val wrapper = mainPanel.getComponent(0) as OnePixelSplitter
        return wrapper.firstComponent as HeaderPanel
    }
}
