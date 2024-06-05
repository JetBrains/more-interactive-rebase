package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.GraphInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.FixupCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import com.jetbrains.interactiveRebase.listeners.IRGitRefreshListener
import git4idea.status.GitRefreshListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Service(Service.Level.PROJECT)
class ModelService(
    private val project: Project,
    private val coroutineScope: CoroutineScope,
    private val commitService: CommitService,
) : Disposable {
    constructor(project: Project, coroutineScope: CoroutineScope) : this(project, coroutineScope, project.service<CommitService>())

    val branchInfo = BranchInfo()

    // TODO: remove?
//    val otherBranchInfo = BranchInfo()
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
     * Adds or removes
     * the commit from the
     * list of selected commits
     */
    fun addOrRemoveCommitSelection(commit: CommitInfo) {
        commit.changes.forEach { change ->
            if (change is FixupCommand || change is SquashCommand) {
                val combinedCommits = project.service<ActionService>().getCombinedCommits(change)
                if (commit.isSelected) {
                    branchInfo.selectedCommits.addAll(combinedCommits)
                    // TODO: remove
//                    otherBranchInfo.selectedCommits.addAll(combinedCommits)
                } else {
                    branchInfo.selectedCommits.removeAll(combinedCommits)
                    // TODO: remove
//                    otherBranchInfo.selectedCommits.removeAll(combinedCommits)
                }
            }
        }
        if (commit.isSelected) {
            branchInfo.addSelectedCommits(commit)
            // TODO: remove
//            otherBranchInfo.addSelectedCommits(commit)
        } else {
            branchInfo.removeSelectedCommits(commit)
            // TODO: remove
//            otherBranchInfo.removeSelectedCommits(commit)
        }
    }

    /**
     * Returns the selected
     * commits
     */
    fun getSelectedCommits(): MutableList<CommitInfo> {
        return branchInfo.selectedCommits
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
        coroutineScope.launch {
            graphService.updateGraphInfo(graphInfo)
        }
    }

    /**
     * Populates the GraphInfo field in order to be able to display the side panel of local branches
     */
    private fun populateLocalBranches() {
        val branchService = project.service<BranchService>()
        coroutineScope.launch {
            graphInfo.branchList = branchService.getBranchesExceptCheckedOut().toMutableList()
        }
    }

    /**
     * Populates the added branch field in the graph info with the given branch
     */
    fun addBranchToGraphInfo(addedBranch: String) {
        coroutineScope.launch {
            graphService.addBranch(graphInfo, addedBranch)
        }
    }

    /**
     * Dispose routine
     */
    override fun dispose() {
    }
}
