package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
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
     * Fetches the branch
     * info inside a
     * coroutine
     */
    fun fetchBranchInfo() {
        coroutineScope.launch {
            val name = commitService.getBranchName()
            val commits = commitService.getCommitInfoForBranch(commitService.getCommits())
            if (branchChange(name, commits)) {
                branchInfo.setName(name)
                branchInfo.setCommits(commits)
                branchInfo.clearSelectedCommits()
                invoker.branchInfo = branchInfo
                invoker.createModel()
            }
        }
    }

    /**
     * Checks if the fetched
     * branch is equal
     * to the current one
     */
    private fun branchChange(
        newName: String,
        newCommits: List<CommitInfo>,
    ): Boolean {
        val commitsIds = branchInfo.commits.map { it.commit.id }.toSet()
        val newCommitsIds = newCommits.map { it.commit.id }.toSet()

        return branchInfo.name != newName || commitsIds != newCommitsIds
    }

    /**
     * Dispose routine
     */
    override fun dispose() {
    }
}
