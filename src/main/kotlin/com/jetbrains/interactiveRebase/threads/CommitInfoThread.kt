package com.jetbrains.interactiveRebase.threads

import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.utils.IRGitUtils

class CommitInfoThread(
    private val project: Project,
    private var branchInfo: BranchInfo,
) : Thread() {
    override fun run() {
        branchInfo.name = IRGitUtils(project).getRepository()?.currentBranchName.toString()
        branchInfo.commits.clear()
        branchInfo.commits.addAll(getCommitInfoForBranch())
    }

    /**
     * Gets the commits for the current branch,
     * and returns them as a list of CommitInfo objects.
     */
    fun getCommitInfoForBranch(): List<CommitInfo> {
        val commits = CommitService(project).getCommits()
        return commits.map { commit ->
            CommitInfo(commit, null)
        }
    }

}
