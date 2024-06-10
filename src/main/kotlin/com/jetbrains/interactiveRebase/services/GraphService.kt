package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.vcs.log.Hash
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
        // update the checked-out branch using the added branch as reference
        commitService.referenceBranchName = addedBranch
        updateBranchInfo(graphInfo.mainBranch)
        graphInfo.mainBranch.isPrimary = true

        if (graphInfo.mainBranch.initialCommits.isEmpty()) {
            return
        }

        // first get commits of the added branch using the checked out branch as reference
        val newBranch = BranchInfo(addedBranch, isPrimary = false, isEnabled = false)
        graphInfo.addedBranch = newBranch
        updateAddedBranchInfo(graphInfo)

        graphInfo.changeAddedBranch(newBranch)
    }

    /**
     * Called when a branch is de-selected from the side panel
     */
    fun removeBranch(graphInfo: GraphInfo) {
        commitService.referenceBranchName = ""
        updateBranchInfo(graphInfo.mainBranch)
        graphInfo.changeAddedBranch(null)
        graphInfo.mainBranch.isPrimary = false
    }

    /**
     * Called from MethodService whenever the model needs to be updated,
     * updates the main BranchInfo and the added one if it exists.
     */
    fun updateGraphInfo(graphInfo: GraphInfo) {
        updateBranchInfo(graphInfo.mainBranch)
        if (graphInfo.addedBranch != null) {
            updateAddedBranchInfo(graphInfo)
        }
    }

    /**
     * Given a commit, gets the parent commit as CommitInfo. Looks at the last commit on the primary branch,
     * Used when trying to find a branching commit after adding a branch,
     * this commit is added as the last element of the secondary branch
     */
    fun getBranchingCommit(graphInfo: GraphInfo): CommitInfo {
        if (graphInfo.mainBranch.currentCommits.isEmpty() || graphInfo.addedBranch == null) {
            throw IRInaccessibleException(
                "Branching-off commit cannot be displayed. Cannot find the added branch or the commits on the primary branch",
            )
        }
        val addedBranch: BranchInfo = graphInfo.addedBranch!!
        val lastInPrimary = graphInfo.mainBranch.currentCommits.last()
        val primaryParents: List<Hash> = lastInPrimary.commit.parents

        if (primaryParents.isEmpty()) {
            throw IRInaccessibleException("Trying to display parents of initial commit")
        }
        var branchingHash: Hash = primaryParents[0]

        // if there are multiple options for a branching commit, compare with added branch and choose the common parent
        if (primaryParents.size > 1 && addedBranch.currentCommits.isNotEmpty()) {
            val addedParents: Set<Hash> = addedBranch.currentCommits.last().commit.parents.toSet()
            val intersection: Set<Hash> = primaryParents.intersect(addedParents)
            branchingHash = if (intersection.isEmpty()) branchingHash else intersection.first()
        }
        val parentCommit = commitService.turnHashToCommit(branchingHash.asString())

        return commitService.getCommitInfoForBranch(listOf(parentCommit)).first()
    }

    /**
     * Given a branchInfo, re-fetches the commits, populates the branch info if it is the first time fetching
     * Also includes the commit before branching off from the reference branch if includeBranchingCommit is true.
     * Refactored from ModelService.
     */
    fun updateBranchInfo(branchInfo: BranchInfo) {
        val name = commitService.getBranchName()

        val commits = commitService.getCommitInfoForBranch(commitService.getCommits(name)).toMutableList()
        if (branchChange(name, commits, branchInfo)) {
            branchInfo.setName(name)
            branchInfo.setCommits(commits)
            branchInfo.clearSelectedCommits()
            invoker.branchInfo = branchInfo
        }
    }

    fun updateAddedBranchInfo(graphInfo: GraphInfo) {
        val addedBranch = graphInfo.addedBranch ?: return
        val commits =
            commitService.getCommitInfoForBranch(
                commitService.getCommitsWithReference(addedBranch.name, graphInfo.mainBranch.name),
            ).toMutableList()
        println("in update addedbranch commtis found $commits current grap $graphInfo")

//        if (graphInfo.mainBranch.currentCommits.isEmpty() && commits.isEmpty()) {
//            throw IRInaccessibleException("Could not find the commit difference between branches")
//        }
        commits.add(getBranchingCommit(graphInfo))

        if (branchChange(addedBranch.name, commits, addedBranch)) {
            addedBranch.setName(addedBranch.name)
            addedBranch.setCommits(commits)
            addedBranch.clearSelectedCommits()
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
