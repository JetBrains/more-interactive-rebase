package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.GraphInfo
import com.jetbrains.interactiveRebase.exceptions.IRInaccessibleException

@Service(Service.Level.PROJECT)
class GraphService(private val project: Project) {
    private var commitService = project.service<CommitService>()
    private val invoker = project.service<RebaseInvoker>()

    /**
     * Secondary constructor for testing
     */
    constructor(project: Project, commitService: CommitService) : this(project) {
        this.commitService = commitService
    }

    /**
     * Updates the main branch info to have reference as the added one
     * Creates a new branch info for the added branch, taking main branch as reference
     * Finds and adds the branching commit to the added branch
     */
    fun addBranch(
        graphInfo: GraphInfo,
        addedBranch: String,
    ) {
        // first get commits of the added branch using the checked out branch as reference
        commitService.referenceBranchName = commitService.getBranchName()
        val newBranch = BranchInfo(addedBranch, isPrimary = false, isWriteable = false)
        updateBranchInfo(newBranch, includeBranchingCommit = true)

        // update the checked-out branch using the added branch as reference
        commitService.referenceBranchName = addedBranch
        updateBranchInfo(graphInfo.mainBranch)
        graphInfo.mainBranch.isPrimary = true

        graphInfo.changeAddedBranch(newBranch)
    }

    fun removeBranch(graphInfo: GraphInfo) {
        graphInfo.mainBranch.isPrimary = false
        commitService.referenceBranchName = ""
        updateBranchInfo(graphInfo.mainBranch)
        graphInfo.changeAddedBranch(null)
    }

    /**
     * Called from MethodService whenever the model needs to be updated,
     * updates the main BranchInfo and the added one if it exists.
     */
    fun updateGraphInfo(graphInfo: GraphInfo) {
        updateBranchInfo(graphInfo.mainBranch)
        if (graphInfo.addedBranch != null) {
            updateBranchInfo(graphInfo.addedBranch!!, includeBranchingCommit = true)
        }
    }

    /**
     * Given a commit, gets the parent commit as CommitInfo.
     * Used when trying to find a branching commit after adding a branch
     */
    fun getBranchingCommit(startingCommit: CommitInfo): CommitInfo {
        val parents = startingCommit.commit.parents
        if (parents.isEmpty() || parents.size > 1) {
            throw IRInaccessibleException("Branching-off commit cannot be displayed")
        }
        val parent = commitService.turnHashToCommit(parents[0].asString())
        return commitService.getCommitInfoForBranch(listOf(parent))[0]
    }

    /**
     * Given a branchInfo, re-fetches the commits, populates the branch info if it is the first time fetching
     * Also includes the commit before branching off from the reference branch if includeBranchingCommit is true.
     * Refactored from ModelService.
     */
    fun updateBranchInfo(
        branchInfo: BranchInfo,
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
            if (branchInfo.isWriteable) {
                invoker.branchInfo = branchInfo
            }
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
