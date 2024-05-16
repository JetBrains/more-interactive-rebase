package com.jetbrains.interactiveRebase.threads

import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.services.CommitService

class BranchInfoThread(
    private val project: Project,
    private var branchInfo: BranchInfo,
    private var service: CommitService,
) : Thread() {
    constructor(project: Project, branchInfo: BranchInfo) : this(project, branchInfo, CommitService(project))

    /**
     * Updates the branchInfo
     */

    override fun run() {
        branchInfo.name = service.getBranchName()
        branchInfo.commits = service.getCommitInfoForBranch(service.getCommits())
        branchInfo.selectedCommits.clear()
    }
}
