package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.OnePixelSplitter
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.CollapseCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.FixupCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.PickCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.RebaseCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.ReorderCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.StopToEditCommand
import com.jetbrains.interactiveRebase.visuals.HeaderPanel
import com.jetbrains.interactiveRebase.visuals.MainPanel

@Service(Service.Level.PROJECT)
class ActionService(project: Project) {
    internal var modelService = project.service<ModelService>()
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
        invoker.undoneCommands.clear()
        modelService.branchInfo.selectedCommits.forEach {
            if (!it.isSquashed) {
                it.setTextFieldEnabledTo(true)
            }
        }
    }

    /**
     * Makes a drop change once the Drop button is clicked
     */
    fun takeDropAction() {
        invoker.undoneCommands.clear()
        val commits = modelService.getSelectedCommits()
        commits.forEach {
                commitInfo ->
            if (!commitInfo.isSquashed) {
                val command = DropCommand(commitInfo)
                commitInfo.addChange(command)
                invoker.addCommand(command)
            }
        }

        modelService.branchInfo.clearSelectedCommits()
    }

    /**
     * Enables the Drop button
     */
    fun checkDrop(e: AnActionEvent) {
        e.presentation.isEnabled = modelService.branchInfo.selectedCommits.isNotEmpty() &&
            modelService.getSelectedCommits().none { commit ->
                commit.getChangesAfterPick().any { change -> change is DropCommand }
            }
    }

    /**
     * Enables reword button
     */
    fun checkReword(e: AnActionEvent) {
        e.presentation.isEnabled = modelService.branchInfo.getActualSelectedCommitsSize() == 1 &&
            modelService.getSelectedCommits().none { commit ->
                commit.getChangesAfterPick().any { change -> change is DropCommand }
            }
    }

    /**
     * Enables stop-to-edit button
     */
    fun checkStopToEdit(e: AnActionEvent) {
        e.presentation.isEnabled = modelService.branchInfo.selectedCommits.isNotEmpty() &&
            modelService.getSelectedCommits().none { commit ->
                commit.getChangesAfterPick().any { change -> change is DropCommand }
            }
    }

    /**
     * Enables fixup or squash button
     */
    fun checkFixupOrSquash(e: AnActionEvent) {
        val notEmpty = modelService.branchInfo.selectedCommits.isNotEmpty()
        val notFirstCommit = !(
                modelService.branchInfo.selectedCommits.size==1 &&
                        modelService.branchInfo.currentCommits.reversed()
                            .indexOf(modelService.branchInfo.selectedCommits[0])==0
                )
        val notDropped = modelService.getSelectedCommits().none { commit ->
            commit.getChangesAfterPick().any { change -> change is DropCommand }
        }


        val notCollapsed = checkParentNotCollapsed()

        e.presentation.isEnabled = notEmpty && notFirstCommit && notDropped && notCollapsed
    }

    fun checkParentNotCollapsed(): Boolean {
        if(modelService.branchInfo.getActualSelectedCommitsSize() != 1) return true

        val commit = modelService.getSelectedCommits().last()
        val index = modelService.branchInfo.currentCommits.indexOf(commit)

        return !modelService.branchInfo.currentCommits[index+1].isCollapsed
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
            if (!commitInfo.isSquashed) {
                val command = StopToEditCommand(commitInfo)
                commitInfo.addChange(command)
                invoker.addCommand(command)
            }
        }
        modelService.branchInfo.clearSelectedCommits()
        invoker.undoneCommands.clear()
    }

    /**
     * Picks the selected commits and removes all changes except for reorders,
     * similar to the logic in the git to-do file for rebasing
     */
    fun performPickAction() {
        invoker.undoneCommands.clear()
        val commits = modelService.getSelectedCommits().reversed()
        commits.forEach { commitInfo ->
            val changes = commitInfo.getChangesAfterPick().iterator()
            while (changes.hasNext()) {
                val change = changes.next()
                if (change is FixupCommand && change.parentCommit == commitInfo) {
                    clearFixupOnPick(change, commitInfo)
                }
                if (change is SquashCommand && change.parentCommit == commitInfo) {
                    clearSquashOnPick(change, commitInfo)
                }
            }
            val pickCommand = PickCommand(commitInfo)
            commitInfo.addChange(pickCommand)
            invoker.addCommand(pickCommand)
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
        invoker.undoneCommands.clear()
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
        invoker.undoneCommands.clear()
        combineCommits(true)
    }

    /**
     * Takes fixup action and
     * creates a fixup command
     */
    fun takeFixupAction() {
        invoker.undoneCommands.clear()
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
            val selectedIndex = modelService.getCurrentCommits().indexOf(parentCommit)
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
        commit.getChangesAfterPick().forEach {
            if (it is FixupCommand || it is SquashCommand) {
                modelService.invoker.removeCommand(it)
                if (modelService.invoker.undoneCommands.contains(it)) {
                    modelService.invoker.undoneCommands.remove(it)
                }
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

    /**
     * Undoes the last action performed by the user.
     * It removes the last command from the list of commands
     * in the invoker, and adds it to a list of "undone" commands.
     *
     * The list of undone commands gets cleared when a new action is performed.
     */
    fun undoLastAction() {
        if (invoker.commands.isEmpty()) return
        val command = invoker.commands.removeLast()
        val commitToBeUndone = command.commitOfCommand()

        if (command is ReorderCommand) {
            undoReorder(commitToBeUndone, command)
        }
        if (command is SquashCommand) {
            undoSquashOrFixup(command, command.squashedCommits, command.parentCommit)
        }
        if (command is FixupCommand) {
            undoSquashOrFixup(command, command.fixupCommits, command.parentCommit)
        }
        if (command is PickCommand) {
            removePickFromSquashOrFixup(commitToBeUndone)
        }

        commitToBeUndone.removeChange(command)
        invoker.undoneCommands.add(command)

        modelService.branchInfo.clearSelectedCommits()
    }

    /**
     * Redoes the last action performed by the user.
     * It removes the last command from the list of "undone" commands
     * in the invoker, and adds it back to the list of commands.
     */
    fun redoLastAction() {
        if (invoker.undoneCommands.isEmpty()) return
        val command = invoker.undoneCommands.removeLast()
        val commitToBeRedone = command.commitOfCommand()

        if (command is ReorderCommand) {
            redoReorder(commitToBeRedone, command)
        }
        if (command is SquashCommand) {
            redoSquash(command)
        }
        if (command is FixupCommand) {
            redoFixup(command)
        }
        if (command is PickCommand) {
            redoPick(commitToBeRedone)
        }

        commitToBeRedone.addChange(command)
        invoker.commands.add(command)

        modelService.branchInfo.clearSelectedCommits()
    }

    /**
     * If the last undone action that was performed by the user was a pick,
     * it checks whether the commit was previously squashed or fixed up.
     *
     * If it was, it deals with it in such a way that the squashed or fixed up commits
     * disappear again from the graph, and they are also picked.
     */
    fun redoPick(commit: CommitInfo) {
        val squashy = commit.changes.lastOrNull { it is SquashCommand } as? SquashCommand
        val fixy = commit.changes.lastOrNull { it is FixupCommand } as? FixupCommand

        if (squashy != null) {
            clearSquashOnPick(squashy, commit)
            squashy.squashedCommits.forEach {
                it.addChange(PickCommand(it))
            }
        }

        if (fixy != null) {
            clearFixupOnPick(fixy, commit)
            fixy.fixupCommits.forEach {
                it.addChange(PickCommand(it))
            }
        }
    }

    /**
     * If the command is a squash or fixup command, it undoes it,
     * by marking the squashed or fixed up commits as not squashed,
     * removing the command from the commits and adding back the commits to tha graph.
     */
    internal fun undoSquashOrFixup(
        command: RebaseCommand,
        commits: List<CommitInfo>,
        parentCommit: CommitInfo,
    ) {
        parentCommit.isSquashed = false
        commits.forEach {
            it.isSquashed = false
            removeSquashFixChange(it)
        }
        parentCommit.removeChange(command)
        parentCommit.setTextFieldEnabledTo(false)

        val currentCommits = modelService.branchInfo.currentCommits
        currentCommits.addAll(
            currentCommits.indexOf(parentCommit),
            commits,
        )
    }

    /**
     * If the undone action is a squash command, we need to add back the logic
     * for hiding the squashed commits.
     */
    internal fun redoSquash(command: SquashCommand) {
        command.squashedCommits.forEach {
            handleCombinedCommits(it, command)
        }
    }

    /**
     * If the undone action is a fixup command, we need to add back the logic
     * for hiding the fixed up commits.
     */
    internal fun redoFixup(command: FixupCommand) {
        command.fixupCommits.forEach {
            handleCombinedCommits(it, command)
        }
    }

    /**
     * If the last undone action that was performed by the user was a pick,
     * it checks whether the commit was previously squashed or fixed up.
     *
     */
    internal fun removePickFromSquashOrFixup(commit: CommitInfo) {
        val squashy = commit.changes.lastOrNull { it is SquashCommand } as? SquashCommand
        val fixy = commit.changes.lastOrNull { it is FixupCommand } as? FixupCommand

        squashy?.let {
            removePickFromSquashed(it.squashedCommits)
        }
        fixy?.let {
            removePickFromSquashed(it.fixupCommits)
        }
    }

    /**
     * This removes all "traces" of the commits being picked from the squashed or fixed up commits.
     */
    internal fun removePickFromSquashed(commits: List<CommitInfo>) {
        commits.forEach {
            it.isSquashed = true
            val pickCommand = it.changes.lastOrNull() as? PickCommand
            pickCommand?.let { command ->
                it.removeChange(command)
                modelService.invoker.removeCommand(command)
                modelService.branchInfo.currentCommits.remove(it)
            }
        }
    }

    /**
     * If the last action that was performed by the user was a reorder,
     * this reorders to the initial state.
     */
    internal fun undoReorder(
        commit: CommitInfo,
        command: ReorderCommand,
    ) {
        commit.setReorderedTo(false)
        mainPanel.branchPanel.branch.updateCurrentCommits(command.newIndex, command.oldIndex, commit)
    }

    /**
     * If the last undone action that was performed by the user was a reorder,
     * this reorders to the previous state.
     */
    internal fun redoReorder(
        commit: CommitInfo,
        command: ReorderCommand,
    ) {
        commit.setReorderedTo(true)
        mainPanel.branchPanel.branch.updateCurrentCommits(command.oldIndex, command.newIndex, commit)
    }

    /**
     * The undo button should be enabled if there are any actions to undo.
     */
    fun checkUndo(e: AnActionEvent) {
        e.presentation.isEnabled = invoker.commands.isNotEmpty()
    }

    /**
     * The redo button should be enabled if there are any actions to redo.
     */
    fun checkRedo(e: AnActionEvent) {
        e.presentation.isEnabled = invoker.undoneCommands.isNotEmpty()
    }

    fun checkCollapse(e: AnActionEvent) {
        // check if there are any already collapsed commits
        val currentCommits = modelService.getCurrentCommits()
        if(modelService.branchInfo.initialCommits.size <= 7){
            e.presentation.isEnabled = false
            return
        }

        if(currentCommits.any { it.isCollapsed }){
            e.presentation.isEnabled = false
            return
        }
        if(modelService.branchInfo.getActualSelectedCommitsSize() == 1){
            e.presentation.isEnabled = false
            return
        }

        if(modelService.getSelectedCommits().isEmpty()){
            e.presentation.isEnabled = true
            return
        }

        e.presentation.isEnabled = checkSelectedCommitsAreInARange()
    }

    fun checkSelectedCommitsAreInARange():Boolean{
        var selectedCommits = modelService.getSelectedCommits()
        val currentCommits = modelService.getCurrentCommits()

        val indexFirstCommit = currentCommits.indexOf(modelService.getHighestSelectedCommit())
        val indexLastCommit = currentCommits.indexOf(modelService.getLowestSelectedCommit())

        val commitsOfRange = currentCommits.subList(indexFirstCommit, indexLastCommit+1)
        selectedCommits = selectedCommits.filter { !it.isSquashed }.toMutableList()
        val res = commitsOfRange.containsAll(selectedCommits)
        return res
    }

    fun expandCollapsedCommits(parentCommit: CommitInfo){
        if(!parentCommit.isCollapsed) return
        parentCommit.isCollapsed = false
        val collapseCommand = parentCommit.changes.filterIsInstance<CollapseCommand>().lastOrNull() as CollapseCommand
        if(collapseCommand == null) return

        parentCommit.removeChange(collapseCommand)
        val index = modelService.branchInfo.currentCommits.indexOf(parentCommit)
        val collapsedCommits = collapseCommand.collapsedCommits
        collapsedCommits.forEach {
            it.isCollapsed = false
            it.removeChange(collapseCommand)
        }
        modelService.branchInfo.addCommitsToCurrentCommits(index, collapsedCommits)
    }

    fun takeCollapseAction(){
        val selectedCommits = modelService.getSelectedCommits()
        selectedCommits.sortBy { modelService.branchInfo.indexOfCommit(it) }

        if(modelService.getSelectedCommits().isEmpty()){
            modelService.branchInfo.collapseCommits()
        }
        else{
            val currentCommits = modelService.getCurrentCommits()
            val indexFirstCommit = currentCommits.indexOf(modelService.getHighestSelectedCommit())
            val indexLastCommit = currentCommits.indexOf(modelService.getLowestSelectedCommit())

            modelService.branchInfo.collapseCommits(indexFirstCommit, indexLastCommit)
        }
    }

    fun getHeaderPanel(): HeaderPanel {
        val wrapper = mainPanel.getComponent(0) as OnePixelSplitter
        return wrapper.firstComponent as HeaderPanel
    }


}
