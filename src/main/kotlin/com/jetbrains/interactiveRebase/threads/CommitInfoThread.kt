package com.jetbrains.interactiveRebase.threads

import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.services.CommitService

class CommitInfoThread(
    private val project: Project,
    private var branchInfo: BranchInfo,
) : Thread() {
    override fun run() {
        CommitService(project).updateBranchInfo(branchInfo)
    }
}
