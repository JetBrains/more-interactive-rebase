package com.jetbrains.interactiveRebase.threads

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.services.ComponentService

class BranchInfoThread(
    private val project: Project,
    private val branchInfo: BranchInfo,
    private val commitService: CommitService,
    private val componentService: ComponentService,
) : Thread() {
    constructor(
        project: Project,
        branchInfo: BranchInfo,
    ) : this(project, branchInfo, project.service<CommitService>(), project.service<ComponentService>())

    /**
     * Updates the branchInfo
     */

    override fun run() {
        val name = commitService.getBranchName()
        val commits = commitService.getCommitInfoForBranch(commitService.getCommits())
        if (branchChange(name, commits)) {
            branchInfo.name = name
            branchInfo.commits = commits
            branchInfo.selectedCommits.clear()

            componentService.repaintMainPanel()
        }
    }

    private fun branchChange(
        newName: String,
        newCommits: List<CommitInfo>,
    ): Boolean {
        val commitsIds = branchInfo.commits.map { it.commit.id }.toSet()
        val newCommitsIds = newCommits.map { it.commit.id }.toSet()

        return branchInfo.name != newName || commitsIds != newCommitsIds
    }
}
