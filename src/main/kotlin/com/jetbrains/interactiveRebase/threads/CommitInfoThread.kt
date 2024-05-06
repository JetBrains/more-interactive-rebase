package com.jetbrains.interactiveRebase.threads

import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.utils.IRGitUtils

class CommitInfoThread(
    private val project: Project,
    private var dto: BranchInfo,
) : Thread() {
    override fun run() {
        dto.branchName = IRGitUtils(project).getRepository()?.currentBranchName.toString()
        dto.commits.clear()
        dto.commits.addAll(CommitService(project).getCommits())
    }
}
