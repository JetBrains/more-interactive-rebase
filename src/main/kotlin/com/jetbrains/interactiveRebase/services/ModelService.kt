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

    val branchInfo = BranchInfo(isCheckedOut = true)
    val graphInfo = GraphInfo(branchInfo)
    private val graphService = project.service<GraphService>()

    internal val invoker = project.service<RebaseInvoker>()

    /**
     * Fetches current branch info
     * on creation and subscribes
     * to the GitRefreshListener
     */
    init {
        fetchBranchInfo()
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
                } else {
                    branchInfo.selectedCommits.removeAll(combinedCommits)
                }
            }
        }
        if (commit.isSelected) {
            branchInfo.addSelectedCommits(commit)
        } else {
            branchInfo.removeSelectedCommits(commit)
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
     * Fetches the branch
     * info inside a
     * coroutine
     */
    fun fetchBranchInfo() {
        coroutineScope.launch {
            graphService.updateGraphInfo(graphInfo)
//            graphService.updateBranchInfo(branchInfo, invoker)
            println("grapph inof is now $graphInfo")
        }
    }

    fun addBranchToGraphInfo(addedBranch: String) {
        coroutineScope.launch {
            graphService.addBranch(graphInfo, addedBranch)
            println("added branch $graphInfo")
        }
    }

    /**
     * Dispose routine
     */
    override fun dispose() {
    }
}
