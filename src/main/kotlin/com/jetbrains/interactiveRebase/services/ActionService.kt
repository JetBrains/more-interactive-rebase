package com.jetbrains.interactiveRebase.services

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.OnePixelSplitter
import com.jetbrains.interactiveRebase.actions.changePanel.CollapseAction
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.CherryCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.CollapseCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.FixupCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.IRCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.PickCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.RebaseCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.ReorderCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.StopToEditCommand
import com.jetbrains.interactiveRebase.utils.takeAction
import com.jetbrains.interactiveRebase.utils.takeActionWithDeselecting
import com.jetbrains.interactiveRebase.visuals.HeaderPanel
import com.jetbrains.interactiveRebase.visuals.MainPanel
import javax.swing.Timer

@Service(Service.Level.PROJECT)
class ActionService(val project: Project) {
    internal var modelService = project.service<ModelService>()
    private var invoker = modelService.invoker
    var mainPanel = MainPanel(project)

    /**
     * Constructor for injection during testing
     */
    constructor(project: Project, modelService: ModelService, invoker: RebaseInvoker) : this(project) {
        this.modelService = modelService
        this.invoker = invoker
    }

    fun checkRebaseIsNotInProgress(): Boolean {
        return !modelService.rebaseInProcess
    }

    /**
     * Enables the text field once the Reword button on the toolbar is pressed
     */
    fun takeRewordAction() {
        project.takeAction {
            modelService.branchInfo.selectedCommits.forEach {
                if (!it.isSquashed) {
                    it.setTextFieldEnabledTo(true)
                }
            }
        }
    }

    /**
     * Makes a drop change once the Drop button is clicked
     */
    fun takeDropAction() {
        project.takeActionWithDeselecting({
            val commits = modelService.getSelectedCommits()
            commits.forEach { commitInfo ->
                if (!commitInfo.isSquashed) {
                    val command = DropCommand(commitInfo)
                    commitInfo.addChange(command)
                    invoker.addCommand(command)
                }
            }
        }, modelService.graphInfo)
    }

    /**
     * Creates a rebase command for a normal rebase
     */
    fun takeNormalRebaseAction() {
        project.takeActionWithDeselecting({
            val command = modelService.graphInfo.addedBranch?.baseCommit?.let { RebaseCommand(it) }
            if (command != null) {
                invoker.addCommand(command)
            }
        }, modelService.graphInfo)
    }

    /**
     * Creates a rebase command for a cherry-picking rebase
     */
    fun takeCherryPickAction() {
        project.takeActionWithDeselecting({
            val commits = modelService.graphInfo.addedBranch?.selectedCommits
            commits?.forEach { commitInfo ->
                prepareCherry(commitInfo)
            }
        }, modelService.graphInfo)
    }

    fun prepareCherry(
        commitInfo: CommitInfo,
        index: Int = 0,
    ) {
        commitInfo.wasCherryPicked = true
        val newCommit =
            CommitInfo(
                commitInfo.commit, project,
                mutableListOf(), false, false,
                false, false, false,
                false, false,
            )
        modelService.graphInfo.mainBranch.currentCommits.add(index, newCommit)
        val command = CherryCommand(commitInfo, newCommit, index)
        newCommit.addChange(command)
        invoker.addCommand(command)
    }

    /**
     * Enables the Cherry Pick button.
     * Commits form the checked out branch cannot be cherry-picked
     */
    fun checkCherryPick(e: AnActionEvent) {
        val addedBranch = modelService.graphInfo.addedBranch
        val index = addedBranch?.currentCommits?.indexOf(addedBranch.baseCommit)
        e.presentation.isEnabled = modelService.branchInfo.selectedCommits.isEmpty() &&
            modelService.areDisabledCommitsSelected() &&
            addedBranch?.selectedCommits!!.all {
                !it.wasCherryPicked &&
                    addedBranch.currentCommits.indexOf(it) < index!!
            }
    }

    /**
     * Enables the Drop button
     * depending on the number of selected commits
     * and the state of the branch.
     * Commits from the added branch cannot be dropped.
     */
    fun checkDrop(e: AnActionEvent) {
        e.presentation.isEnabled = modelService.branchInfo.selectedCommits.isNotEmpty() &&
            !modelService.areDisabledCommitsSelected() && checkRebaseIsNotInProgress() &&
            modelService.getSelectedCommits().none { commit ->
                commit.getChangesAfterPick().any { change -> change is DropCommand }
            }
    }

    /**
     * Enables reword button
     * depending on the number of selected commits
     * and the state of the branch.
     * Commits from the added branch cannot be reworded.
     */
    fun checkReword(e: AnActionEvent) {
        e.presentation.isEnabled = modelService.branchInfo.getActualSelectedCommitsSize() == 1 &&
            !modelService.areDisabledCommitsSelected() && checkRebaseIsNotInProgress() &&
            modelService.getSelectedCommits().none { commit ->
                commit.getChangesAfterPick().any { change -> change is DropCommand }
            }
    }

    /**
     * Enables stop-to-edit button
     * depending on the number of selected commits
     * and the state of the branch.
     * Commits from the added branch cannot be edited.
     */
    fun checkStopToEdit(e: AnActionEvent) {
        e.presentation.isEnabled = modelService.branchInfo.selectedCommits.isNotEmpty() &&
            !modelService.areDisabledCommitsSelected() && checkRebaseIsNotInProgress() &&
            modelService.getSelectedCommits().none { commit ->
                commit.getChangesAfterPick().any { change -> change is DropCommand }
            }
    }

    /**
     * Enables fixup or squash button
     * depending on the number of selected commits
     * and the state of the branch.
     * Commits from the added branch cannot be squashed.
     */
    fun checkFixupOrSquash(e: AnActionEvent) {
        val notEmpty = modelService.branchInfo.selectedCommits.isNotEmpty()
        val notFirstCommit =
            !(
                modelService.branchInfo.selectedCommits.size==1 &&
                    modelService.branchInfo.currentCommits.reversed()
                        .indexOf(modelService.branchInfo.selectedCommits[0])==0
            )
        val notDropped =
            modelService.getSelectedCommits().none { commit ->
                commit.getChangesAfterPick().any { change -> change is DropCommand }
            }

        val validParent = checkValidParent()

        e.presentation.isEnabled = notEmpty &&
            notFirstCommit && notDropped &&
            validParent && !modelService.areDisabledCommitsSelected() &&
            checkRebaseIsNotInProgress()
    }

    /**
     * Checks that the if the commit to
     * squash/fixup into would be a valid
     * parent. Not dropped or collapsed
     */

    fun checkValidParent(): Boolean {
        if (modelService.branchInfo.getActualSelectedCommitsSize() != 1) return true

        var commit = modelService.getLastSelectedCommit()

        if (commit == modelService.getCurrentCommits().last()) {
            return false
        }

        commit = getParent()

        return !commit.isCollapsed && commit.getChangesAfterPick().filterIsInstance<DropCommand>().isEmpty()
    }

    /**
     * Gets parent of current commit to
     * squash into, it should not be dropped
     * nor collapsed
     */

    fun getParent(): CommitInfo {
        var commit = modelService.getLastSelectedCommit()

        var index = modelService.getCurrentCommits().indexOf(commit) + 1
        commit = modelService.getCurrentCommits()[index]
        while (commit.getChangesAfterPick().filterIsInstance<DropCommand>().isNotEmpty() &&
            index < modelService.getCurrentCommits().size - 1
        ) {
            index++
            commit = modelService.getCurrentCommits()[index]
        }

        return commit
    }

    /**
     * Enables pick button
     * depending on the number of selected commits
     * and the state of the branch.
     * Commits from the added branch cannot be picked.
     */
    fun checkPick(e: AnActionEvent) {
        e.presentation.isEnabled = modelService.branchInfo.selectedCommits.isNotEmpty() &&
            !modelService.areDisabledCommitsSelected() && checkRebaseIsNotInProgress()
    }

    /**
     * Enables rebase button
     * depending on the number of selected commits
     * and the state of the branch.
     * Commits from the checked-out branch cannot be picked.
     */
    fun checkNormalRebaseAction(e: AnActionEvent) {
        e.presentation.isEnabled = modelService.graphInfo.addedBranch != null &&
            (
                !modelService.areDisabledCommitsSelected() ||
                    (
                        modelService.graphInfo.addedBranch?.selectedCommits?.size!! <= 1 &&
                            modelService.graphInfo.addedBranch?.selectedCommits!![0] !=
                            modelService.graphInfo.addedBranch!!.baseCommit
                    )
            )
    }

    /**
     * Enables rebase button
     */
    fun checkRebaseAndReset(e: AnActionEvent) {
        e.presentation.isEnabled = invoker.commands.size != 0
    }

    /**
     * Adds a visual change for a commit that has to be stopped to edit
     */
    fun takeStopToEditAction() {
        project.takeActionWithDeselecting({
            val commits = modelService.getSelectedCommits()
            commits.forEach { commitInfo ->
                if (!commitInfo.isSquashed) {
                    val command = StopToEditCommand(commitInfo)
                    commitInfo.addChange(command)
                    invoker.addCommand(command)
                }
            }
        }, modelService.graphInfo)
    }

    /**
     * Picks the selected commits and removes all changes except for reorders,
     * similar to the logic in the git to-do file for rebasing
     */
    fun performPickAction() {
        project.takeActionWithDeselecting({
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
        }, modelService.graphInfo)
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
        project.takeActionWithDeselecting({
            invoker.commands = mutableListOf()
            invoker.undoneCommands.clear()
            val currentBranchInfo = invoker.branchInfo
            invoker.branchInfo.currentCommits = currentBranchInfo.initialCommits.toMutableList()
            invoker.branchInfo.initialCommits.forEach { commitInfo ->
                resetCommitInfo(commitInfo)
            }
            modelService.graphInfo.addedBranch?.initialCommits?.forEach { commitInfo ->
                resetAddedCommitInfo(commitInfo)
            }
            modelService.graphInfo.mainBranch.isRebased = false
            modelService.graphInfo.addedBranch?.baseCommit =
                modelService.graphInfo.addedBranch?.currentCommits?.last()
            invoker.branchInfo.clearSelectedCommits()
            modelService.graphInfo.addedBranch?.clearSelectedCommits()
            takeCollapseAction()
        }, modelService.graphInfo)
    }

    fun resetAddedCommitInfo(commitInfo: CommitInfo) {
        val collapseCommand = commitInfo.changes.find { it is CollapseCommand }
        commitInfo.changes.clear()
        if (collapseCommand != null) {
            commitInfo.addChange(collapseCommand)
        }
        commitInfo.isSelected = false
        commitInfo.wasCherryPicked = false
    }

    /**
     * Resets all fields of a CommitInfo
     */
    fun resetCommitInfo(commitInfo: CommitInfo) {
        commitInfo.changes.clear()
        commitInfo.isSelected = false
        commitInfo.isSquashed = false
        commitInfo.isTextFieldEnabled = false
        commitInfo.isDragged = false
        commitInfo.isReordered = false
        commitInfo.isHovered = false
        commitInfo.isCollapsed = false
    }

    /**
     * Takes squash action and
     * creates a squash command
     */
    fun takeSquashAction() {
        project.takeAction {
            combineCommits(true)
        }
    }

    /**
     * Takes fixup action and
     * creates a fixup command
     */
    fun takeFixupAction() {
        project.takeAction {
            combineCommits(false)
        }
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
            parentCommit = getParent()
        }
        selectedCommits.remove(parentCommit)
        val fixupCommits = cleanSelectedCommits(parentCommit, selectedCommits)
        var command: IRCommand = FixupCommand(parentCommit, fixupCommits)

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
        command: IRCommand,
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
            addIfNotExists(ret, it)
        }

        parent.getChangesAfterPick().forEach {
                change ->
            if (change is SquashCommand) {
                change.squashedCommits.forEach {
                    removeSquashFixChange(it)
                    addIfNotExists(ret, it)
                }
            } else if (change is FixupCommand) {
                change.fixupCommits.forEach {
                    removeSquashFixChange(it)
                    addIfNotExists(ret, it)
                }
            }
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
        val changesToRemove = mutableListOf<IRCommand>()
        commit.getChangesAfterPick().forEach {
            if (it is FixupCommand || it is SquashCommand) {
                modelService.invoker.removeCommand(it)
                if (modelService.invoker.undoneCommands.contains(it)) {
                    modelService.invoker.undoneCommands.removeIf { x -> x === it }
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
    fun getCombinedCommits(change: IRCommand): List<CommitInfo> {
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
    internal fun undoLastAction() {
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
        if (command is CherryCommand) {
            undoCherryPick(commitToBeUndone, command)
        }
        if (command is RebaseCommand) {
            undoRebase()
        } else {
            commitToBeUndone.removeChange(command)
        }
        invoker.undoneCommands.add(command)

        modelService.branchInfo.clearSelectedCommits()
    }

    private fun undoRebase() {
        val base = modelService.graphInfo.addedBranch?.currentCommits?.last()
        val lastRebaseCommandCommit =
            invoker.commands
                .findLast { command -> command is RebaseCommand }?.commitOfCommand()
        modelService.graphInfo.addedBranch?.baseCommit = lastRebaseCommandCommit ?: base
        modelService.graphInfo.mainBranch.isRebased = false
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
        if (command is CherryCommand) {
            redoCherryPick(commitToBeRedone, command)
        }
        if (command is RebaseCommand) {
            redoRebase(commitToBeRedone)
        } else {
            commitToBeRedone.addChange(command)
        }

        invoker.commands.add(command)

        modelService.branchInfo.clearSelectedCommits()
    }

    private fun redoRebase(commitToBeRedone: CommitInfo) {
        modelService.graphInfo.addedBranch?.baseCommit = commitToBeRedone
        modelService.graphInfo.mainBranch.isRebased = true
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
     * If the last action that was performed by the user was a cherry-pick,
     * this removes it from the checked out branch.
     */
    internal fun undoCherryPick(
        commit: CommitInfo,
        command: CherryCommand,
    ) {
        modelService.graphInfo.mainBranch.currentCommits.remove(commit)
        val original =
            modelService.graphInfo.addedBranch?.currentCommits?.filter { it.wasCherryPicked }?.find {
                it.commit == commit.commit
            }
        original?.wasCherryPicked = false
        // mainPanel.graphPanel.addedBranchPanel?.updateCommits()
    }

    /**
     * If the last action that was undone by the user was a cherry-pick,
     * this adds it back to the checked out branch.
     */
    internal fun redoCherryPick(
        commit: CommitInfo,
        command: CherryCommand,
    ) {
        val original = command.baseCommit
        val index = command.index
        modelService.graphInfo.mainBranch.currentCommits.add(index, commit)
        original.wasCherryPicked = true
        mainPanel.graphPanel.addedBranchPanel?.branchPanel!!.updateCommits()
    }

    /**
     * If the command is a squash or fixup command, it undoes it,
     * by marking the squashed or fixed up commits as not squashed,
     * removing the command from the commits and adding back the commits to tha graph.
     */
    internal fun undoSquashOrFixup(
        command: IRCommand,
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
        mainPanel.graphPanel.mainBranchPanel.branch.updateCurrentCommits(command.newIndex, command.oldIndex, commit)
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
        mainPanel.graphPanel.mainBranchPanel.branch.updateCurrentCommits(command.oldIndex, command.newIndex, commit)
    }

    /**
     * The undo button should be enabled if there are any actions to undo.
     */
    fun checkIfChangesMade(e: AnActionEvent) {
        e.presentation.isEnabled = invoker.commands.isNotEmpty()
    }

    /**
     * The redo button should be enabled if there are any actions to redo.
     */
    fun checkRedo(e: AnActionEvent) {
        e.presentation.isEnabled = invoker.undoneCommands.isNotEmpty()
    }

    /**
     * The collapse button should be enabled if the action is valid, which is in the cases:
     * - there are no already collapsed commits
     * - there are more than 7 commits
     * - there is no selected commit OR
     * - there are at least 2 selected commits, which are in a range.
     * - commits were automatically collapsed again after expanding because of their length
     */
    fun checkCollapse(e: AnActionEvent) {
        // check if nested-collapsing was present and enable collapsing to initial state
        if (checkIfNestedCollapsingPresent() && modelService.getSelectedBranch().getActualSelectedCommitsSize() == 0) {
            e.presentation.isEnabled = true
            return
        }
        // check if there are any already collapsed commits
        if (modelService.branchInfo.initialCommits.size <= 7) {
            if (modelService.graphInfo.addedBranch == null) {
                e.presentation.isEnabled = false
                return
            } else if (modelService.graphInfo.addedBranch!!.initialCommits.size <= 7) {
                e.presentation.isEnabled = false
                return
            }
        }

        if (modelService.graphInfo.mainBranch.currentCommits.any { it.isCollapsed }) {
            if (modelService.graphInfo.addedBranch != null) {
                if (modelService.graphInfo.addedBranch!!.currentCommits.any { it.isCollapsed }) {
                    e.presentation.isEnabled = false
                    return
                }
            } else {
                e.presentation.isEnabled = false
                return
            }
        }
        if (modelService.getSelectedBranch().getActualSelectedCommitsSize() == 1) {
            e.presentation.isEnabled = false
            return
        }

        if (modelService.getSelectedCommits().isEmpty()) {
            e.presentation.isEnabled = true
            return
        }

        e.presentation.isEnabled = checkSelectedCommitsAreInARange()
    }

    /**
     * Returns true if any of the branches has
     */
    fun checkIfNestedCollapsingPresent(): Boolean {
        val secondBranch = if (modelService.graphInfo.addedBranch == null) false else modelService.graphInfo.addedBranch!!.isNestedCollapsed
        return modelService.graphInfo.mainBranch.isNestedCollapsed || secondBranch
    }

    /**
     * Checks whether the selected commits are in a range.
     */
    fun checkSelectedCommitsAreInARange(): Boolean {
        var selectedCommits = modelService.getSelectedCommits()
        val currentCommits = modelService.getCurrentCommits()

        val indexFirstCommit = currentCommits.indexOf(modelService.getHighestSelectedCommit())
        val indexLastCommit = currentCommits.indexOf(modelService.getLowestSelectedCommit())

        val commitsOfRange = currentCommits.subList(indexFirstCommit, indexLastCommit + 1)
        selectedCommits = selectedCommits.filter { !it.isSquashed }.toMutableList()
        return selectedCommits.containsAll(commitsOfRange)
    }

    /**
     * Expands the collapsed commits, by removing the collapse command,
     * setting the isCollapsed flag to false,
     * and adding back the collapsed commits
     * to the list of current commits.
     */
    fun expandCollapsedCommits(
        parentCommit: CommitInfo,
        branch: BranchInfo,
        enableNestedCollapsing: Boolean = true,
    ) {
        project.takeActionWithDeselecting({
            if (!parentCommit.isCollapsed) return@takeActionWithDeselecting
            parentCommit.isCollapsed = false
            val collapseCommand =
                parentCommit.changes.filterIsInstance<CollapseCommand>().lastOrNull() as CollapseCommand
            if (collapseCommand == null) return@takeActionWithDeselecting

            var collapsedCommits = collapseCommand.collapsedCommits
            parentCommit.changes.asReversed().removeIf { it === collapseCommand }
            val index = branch.currentCommits.indexOf(parentCommit)

            collapsedCommits.forEach { commit ->
                commit.isCollapsed = false
                commit.changes.asReversed().removeIf { it === collapseCommand }
            }

            if (collapsedCommits.size >= 30 && enableNestedCollapsing) {
                collapsedCommits = collapseAgainIfNeeded(collapsedCommits, branch, parentCommit)
            } else {
                branch.isNestedCollapsed = false
            }
            branch.addCommitsToCurrentCommits(index, collapsedCommits)
        }, modelService.graphInfo)

        if (branch.currentCommits.size >= 300) {
            createNotification()
        }
    }

    fun createNotification() {
        val notification =
            Notification(
                "Dependencies",
                "Consider collapsing commits to improve performance.",
                NotificationType.INFORMATION,
            )
        notification.setTitle("Reduced performance due to large branch")
        notification.addAction(CollapseAction())
        notification.notify(project)
        Timer(15000) { notification.expire() }.apply {
            isRepeats = false
            start()
        }
    }

    /**
     * Called during expanding, if commits that are expanded is more than 30, only the first 20 are shown and the rest are collapsed again.
     * Takes all initially collapsed commits os alreadyCollapsed, and the parent that is not included in this list as parentCommit
     */
    private fun collapseAgainIfNeeded(
        alreadyCollapsed: List<CommitInfo>,
        branch: BranchInfo,
        parentCommit: CommitInfo,
    ): MutableList<CommitInfo> {
        if (alreadyCollapsed.size < 30) return alreadyCollapsed.toMutableList()
        branch.isNestedCollapsed = true
        val numberToExpand = 20
        val toExpand = alreadyCollapsed.take(numberToExpand)
        val collapseAgain = alreadyCollapsed.drop(numberToExpand)

        branch.collapseCommitsWithList(collapseAgain, parentCommit)
        return toExpand.toMutableList()
    }

    /**
     * Takes the collapse action, which collapses the selected commits, by either
     * keeping the first 5 commits and last commit, or the selected commits.
     */
    fun takeCollapseAction() {
        project.takeActionWithDeselecting({
            if (modelService.getSelectedCommits().isEmpty()) {
                autoCollapseBranch(modelService.graphInfo.mainBranch)
                autoCollapseBranch(modelService.graphInfo.addedBranch)
            } else {
                val selectedCommits = modelService.getSelectedCommits()
                selectedCommits.sortBy { modelService.getSelectedBranch().indexOfCommit(it) }

                val currentCommits = modelService.getCurrentCommits()
                val indexFirstCommit = currentCommits.indexOf(modelService.getHighestSelectedCommit())
                val indexLastCommit = currentCommits.indexOf(modelService.getLowestSelectedCommit())

                modelService.getSelectedBranch().collapseCommits(indexFirstCommit, indexLastCommit)
            }
        }, modelService.graphInfo)
    }

    fun autoCollapseBranch(branch: BranchInfo?) {
        if (branch == null) return
        // expand first if there are nested collapses present
        val possibleParent = branch.currentCommits.find { it.isCollapsed }
        if (possibleParent != null) {
            expandCollapsedCommits(possibleParent, branch, enableNestedCollapsing = false)
            branch.isNestedCollapsed = false
        }

        if (branch.currentCommits.none { it.isCollapsed }) {
            branch.collapseCommits()
        }
    }

    /**
     * Gets the header panel instance
     */
    fun getHeaderPanel(): HeaderPanel {
        val wrapper = mainPanel.getComponent(0) as OnePixelSplitter
        return wrapper.firstComponent as HeaderPanel
    }

    /**
     * Removes the rebase + reset process panel and shows the continue + abort buttons
     */
    fun switchToRebaseProcessPanel() {
        val headerPanel = getHeaderPanel()
        headerPanel.changeActionsPanel.isVisible = false
        headerPanel.rebaseProcessPanel.isVisible = true
        headerPanel.revalidate()
        headerPanel.repaint()
    }

    /**
     * Removes the continue + abort process panel and shows the rebase + reset buttons
     */
    fun switchToChangeButtonsIfNeeded() {
        val headerPanel = getHeaderPanel()
        headerPanel.changeActionsPanel.isVisible = true
        headerPanel.rebaseProcessPanel.isVisible = false
        headerPanel.revalidate()
        headerPanel.repaint()
    }

    fun <T> addIfNotExists(
        list: MutableList<T>,
        element: T,
    ) {
        if (!list.contains(element)) {
            list.add(element)
        }
    }
}
