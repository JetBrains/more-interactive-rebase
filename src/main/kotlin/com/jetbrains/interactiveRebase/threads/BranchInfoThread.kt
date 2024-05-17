package com.jetbrains.interactiveRebase.threads

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.services.RebaseInvoker

class BranchInfoThread(
    private val project: Project,
    private val branchInfo: BranchInfo,
    private val commitService: CommitService,
    private val componentService: ComponentService,
    private var invoker: RebaseInvoker
) : Thread() {
    constructor(
        project: Project,
        branchInfo: BranchInfo,
        invoker: RebaseInvoker
    ) : this(project, branchInfo, project.service<CommitService>(),
            project.service<ComponentService>(), project.service<RebaseInvoker>())

    /**
     * Updates the branchInfo
     */

    override fun run() {
        val name = commitService.getBranchName()
        val commits = commitService.getCommitInfoForBranch(commitService.getCommits())
        if (branchInfo.name != name) {
            branchInfo.name = name
            branchInfo.commits = commits
            branchInfo.selectedCommits.clear()
            if(componentService.first){
                invoker.branchInfo = branchInfo
                componentService.first = false
            }

            componentService.isDirty = true

        }
    }
}
