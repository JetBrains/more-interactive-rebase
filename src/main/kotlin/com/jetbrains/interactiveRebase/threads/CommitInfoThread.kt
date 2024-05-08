package com.jetbrains.interactiveRebase.threads

import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.utils.IRGitUtils

class CommitInfoThread(
    private val project: Project,
    private var branchInfo: BranchInfo,
) : Thread() {
    override fun run() {
        branchInfo.branchName = IRGitUtils(project).getRepository()?.currentBranchName.toString()
        branchInfo.commits.clear()
        branchInfo.commits.addAll(CommitService(project).getCommits())
    }
}
