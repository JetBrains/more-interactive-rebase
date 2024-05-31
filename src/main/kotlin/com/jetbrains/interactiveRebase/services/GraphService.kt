package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.components.services
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.GraphInfo
import com.jetbrains.interactiveRebase.exceptions.IRInaccessibleException

@Service(Service.Level.PROJECT)
class GraphService(private val project: Project) {
    private val commitService = project.service<CommitService>()

    fun addBranch(
        graphInfo: GraphInfo,
        addedBranch: String,
    ) {
        println("in add branch to add $addedBranch")
        // first get commits of the added branch using the checked out branch as reference
        commitService.referenceBranchName = commitService.getBranchName()
        val newBranch = BranchInfo(addedBranch)
        updateBranchInfo(newBranch, includeBranchingCommit = true)
        graphInfo.addedBranch = newBranch

        println("updated added in graphInfo it is ${graphInfo.addedBranch} new branch is $newBranch")

        // update the checked-out branch using the added branch as reference
        commitService.referenceBranchName = addedBranch
        updateBranchInfo(graphInfo.mainBranch)
    }

    fun updateGraphInfo(graphInfo: GraphInfo) {
        updateBranchInfo(graphInfo.mainBranch)
        if (graphInfo.addedBranch != null) {
            updateBranchInfo(graphInfo.addedBranch!!, includeBranchingCommit = true)
        }
    }

    fun getBranchingCommit(startingCommit: CommitInfo): CommitInfo {
        val parents = startingCommit.commit.parents
        if (parents.isEmpty() || parents.size > 1) {
            throw IRInaccessibleException("Branching-off commit cannot be displayed")
        }
        val parent = commitService.turnHashToCommit(parents[0].asString())
        val ret = commitService.getCommitInfoForBranch(listOf(parent))[0]
        println("got branching $ret")
        return ret
    }

    fun updateBranchInfo(
        branchInfo: BranchInfo,
        invoker: RebaseInvoker = project.service<RebaseInvoker>(),
        includeBranchingCommit: Boolean = false,
    ) {
        val name = branchInfo.name.ifEmpty { commitService.getBranchName() }

        val commits = commitService.getCommitInfoForBranch(commitService.getCommits(name)).toMutableList()
        if (includeBranchingCommit) {
            commits.add(getBranchingCommit(commits.last()))
        }
        if (branchChange(name, commits, branchInfo)) {
            branchInfo.setName(name)
            branchInfo.setCommits(commits)
            branchInfo.clearSelectedCommits()
            invoker.branchInfo = branchInfo
            // invoker.createModel()
        }
    }

    /**
     * Checks if the fetched
     * branch is equal
     * to the current one returns true if branch is different
     */
    private fun branchChange(
        newName: String,
        newCommits: List<CommitInfo>,
        branchInfo: BranchInfo,
    ): Boolean {
        val commitsIds = branchInfo.initialCommits.map { it.commit.id }.toSet()
        val newCommitsIds = newCommits.map { it.commit.id }.toSet()

        return branchInfo.name != newName || commitsIds != newCommitsIds
    }
}
