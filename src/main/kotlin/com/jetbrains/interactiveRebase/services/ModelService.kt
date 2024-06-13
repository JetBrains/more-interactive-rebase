package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.GraphInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.FixupCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.ReorderCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import com.jetbrains.interactiveRebase.listeners.IRGitRefreshListener
import git4idea.status.GitRefreshListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class ModelService(
    private val project: Project,
    private val coroutineScope: CoroutineScope,
    private val commitService: CommitService,
) : Disposable {
    constructor(project: Project, coroutineScope: CoroutineScope) : this(project, coroutineScope, project.service<CommitService>())

    val branchInfo = BranchInfo()
    val graphInfo = GraphInfo(branchInfo)
    private val graphService = project.service<GraphService>()

    internal val invoker = project.service<RebaseInvoker>()

    /**
     * Fetches current branch info
     * on creation and subscribes
     * to the GitRefreshListener
     */
    init {
        fetchGraphInfo()
        populateLocalBranches()
        project.messageBus.connect(this).subscribe(GitRefreshListener.TOPIC, IRGitRefreshListener(project))
    }

    /**
     * Selects the single passed in
     * commit by also deselecting all
     * currently selected commits
     */
    fun selectSingleCommit(
        commit: CommitInfo,
        branchInfo: BranchInfo,
    ) {
        branchInfo.clearSelectedCommits()
        addToSelectedCommits(commit, branchInfo)
    }

    /**
     * Adds a commit to the list
     * of selected commits without
     * deselecting the rest of the
     * commits
     */
    fun addToSelectedCommits(
        commit: CommitInfo,
        branchInfo: BranchInfo,
    ) {
        if (commit.isCollapsed) return
        commit.isSelected = true
        branchInfo.addSelectedCommits(commit)
        commit.getChangesAfterPick().forEach { change ->
            if (change is FixupCommand || change is SquashCommand) {
                project.service<ActionService>().getCombinedCommits(change).forEach {
                    branchInfo.addSelectedCommits(it)
                }
            }
        }
    }

    /**
     * Removes commit from
     * list of selected commits
     * for a given branch
     */
    fun removeFromSelectedCommits(
        commit: CommitInfo,
        branchInfo: BranchInfo,
    ) {
        commit.isSelected = false
        if (commit.isCollapsed) return
        branchInfo.removeSelectedCommits(commit)
    }

    /**
     * Marks a commit as a reordered by
     * 1. sets the isReordered flag to true
     * 2. adds a ReorderCommand
     * to the visual changes applied to the commit
     * 3. adds the Reorder Command to the Invoker
     * that holds an overview of all staged changes.
     */
    internal fun markCommitAsReordered(
        commit: CommitInfo,
        oldIndex: Int,
        newIndex: Int,
    ) {
        commit.setReorderedTo(true)
        val command =
            ReorderCommand(
                commit,
                oldIndex,
                newIndex,
            )
        commit.addChange(command)
        project.service<RebaseInvoker>().addCommand(command)
    }

    /**
     * Returns the selected
     * commits
     */
    fun getSelectedCommits(): MutableList<CommitInfo> {
        return branchInfo.selectedCommits
    }

    /**
     * Returns the selected commit which is the lowest visually in the list.
     */
    fun getLowestSelectedCommit(): CommitInfo {
        var commit = branchInfo.selectedCommits[0]
        var index = branchInfo.currentCommits.indexOf(commit)

        branchInfo.selectedCommits.forEach {
            if (branchInfo.currentCommits.indexOf(it) > index && !it.isSquashed) {
                commit = it
                index = branchInfo.currentCommits.indexOf(it)
            }
        }

        return commit
    }

    /**
     * Returns the selected commit which is the highest visually in the list.
     */
    fun getHighestSelectedCommit(): CommitInfo {
        var commit = branchInfo.selectedCommits[0]
        var index = branchInfo.currentCommits.indexOf(commit)

        branchInfo.selectedCommits.forEach {
            if (branchInfo.currentCommits.indexOf(it) < index && !it.isSquashed) {
                commit = it
                index = branchInfo.currentCommits.indexOf(it)
            }
        }

        return commit
    }

    /**
     * Returns the last commit that is selected
     * but is not squashed or fixed up
     */
    fun getLastSelectedCommit(branchInfo: BranchInfo): CommitInfo {
        var commit = branchInfo.selectedCommits.last()

        // Ensure that the commit we are moving is actually displayed
        while (commit.isSquashed) {
            val index = branchInfo.selectedCommits.indexOf(commit)
            commit = branchInfo.selectedCommits[index - 1]
        }

        return commit
    }

    /**
     * Returns the current
     * displayed commits
     */
    fun getCurrentCommits(): MutableList<CommitInfo> {
        return branchInfo.currentCommits
    }

    /**
     * Fetches and updates the graph
     * info inside a
     * coroutine
     */
    fun fetchGraphInfo() {
        coroutineScope.launch(Dispatchers.IO) {
            graphService.updateGraphInfo(graphInfo)
        }
    }

    /**
     * Populates the GraphInfo field in order to be able to display the side panel of local branches
     */
    fun populateLocalBranches() {
        val branchService = project.service<BranchService>()
        coroutineScope.launch(Dispatchers.IO) {
            graphInfo.branchList = branchService.getBranchesExceptCheckedOut().toMutableList()
        }
    }

    /**
     * Populates the added branch field in the graph info with the given branch
     */
    fun addSecondBranchToGraphInfo(addedBranch: String) {
        coroutineScope.launch(Dispatchers.IO) {
            graphService.addBranch(graphInfo, addedBranch)
        }
    }

    /**
     * Removes the added branch field in the graph info
     */
    fun removeSecondBranchFromGraphInfo() {
        coroutineScope.launch(Dispatchers.IO) {
            graphService.removeBranch(graphInfo)
        }
    }

    /**
     * Dispose routine
     */
    override fun dispose() {
    }
}
