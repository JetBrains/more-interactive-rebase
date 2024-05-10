package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.exceptions.IRInaccessibleException
import com.jetbrains.interactiveRebase.utils.IRGitUtils
import com.jetbrains.interactiveRebase.utils.consumers.CommitConsumer
import com.jetbrains.interactiveRebase.utils.consumers.GeneralCommitConsumer
import git4idea.GitCommit
import git4idea.commands.GitCommand
import git4idea.commands.GitCommandResult
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository

@Service(Service.Level.PROJECT)
class CommitService(private val project: Project, private val gitUtils: IRGitUtils, private val branchSer: BranchService) {
    /**
     * Usually the primary branch master or main, can be configured
     */
    var referenceBranchName = "main"
    constructor(project: Project) : this(project, IRGitUtils(project), BranchService(project))

    /**
     * Finds the current branch and returns all the commits that are on the current branch and not on the reference branch.
     * If the reference branch is the current branch only the maximum amount of commits are displayed
     * Reference branch is dynamically set to master or main according to the repo
     */
    fun getCommits(): List<GitCommit> {
        referenceBranchName = branchSer.getDefaultReferenceBranchName()
        val repo =
            gitUtils.getRepository()
                ?: throw IRInaccessibleException("Repository cannot be accessed")

        val branchName = repo.currentBranchName ?: throw IRInaccessibleException("Branch cannot be accessed")
        val consumer = GeneralCommitConsumer()
        return getDisplayableCommitsOfBranch(branchName, repo, consumer)
    }

    /**
     * Gets the commits in the given branch that are not on the reference branch, caps them to the maximum size at the consumer.
     * If the current branch is the reference branch or has been merged to the reference branch, gets all commits until reaching the cap
     */
    fun getDisplayableCommitsOfBranch(
        branchName: String,
        repo: GitRepository,
        consumer: CommitConsumer,
    ): List<GitCommit> {
        if (branchName == referenceBranchName || branchSer.isBranchMerged(branchName)) {
            gitUtils.getCommitsOfBranch(repo, consumer)
        } else {
            gitUtils.getCommitDifferenceBetweenBranches(branchName, referenceBranchName, repo, consumer)
        }
        return consumer.commits
    }

    /**
     * Maps GitCommits to CommitInfo objects
     */
    fun getCommitInfoForBranch(commits: List<GitCommit>): List<CommitInfo> {
        return commits.map { commit ->
            CommitInfo(commit, project, null)
        }
    }

    /**
     * Gets branchname from utils
     */
    fun getBranchName(): String {
        return gitUtils.getRepository()?.currentBranchName.toString()
    }
}
