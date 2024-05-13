package com.jetbrains.interactiveRebase.threads

import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.utils.IRGitUtils

class BranchInfoThread(
    private val project: Project,
    private var branchInfo: BranchInfo,
    private var service: CommitService,
) : Thread() {
    constructor(project: Project, branchInfo: BranchInfo) : this(project, branchInfo, CommitService(project))

    override fun run() {
        branchInfo.name = IRGitUtils(project).getRepository()?.currentBranchName.toString()
        branchInfo.commits = service.getCommitInfoForBranch(service.getCommits())
    }
}
