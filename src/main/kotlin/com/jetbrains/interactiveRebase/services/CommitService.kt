package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.exceptions.IRInaccessibleException
import com.jetbrains.interactiveRebase.utils.IRGitUtils
import com.jetbrains.interactiveRebase.utils.consumers.CommitConsumer
import git4idea.GitCommit
import git4idea.repo.GitRepository

@Service(Service.Level.PROJECT)
class CommitService(private val project: Project, private val IRGitUtils: IRGitUtils) {
    var referenceBranchName = "origin/master"
    constructor(project: Project) : this(project, IRGitUtils(project))

    /**
     * Finds the current branch and returns all the commits that are on the current branch and not on the reference branch.
     * If the reference branch is the current branch only the maximum amount of commits are displayed
     */
    fun getCommits(): List<GitCommit> {
        val repo =
            IRGitUtils.getRepository()
                ?: throw IRInaccessibleException("GitRepository cannot be accessed")

        val branchName = repo.currentBranchName ?: throw IRInaccessibleException("cannot access current branch")
        val consumer = GeneralCommitConsumer()
        return getDisplayableCommitsOfBranch(branchName, repo, consumer)
    }

    /**
     * Gets the commits in the given branch that are not on the reference branch, caps them to the maximum size at the consumer.
     */
    fun getDisplayableCommitsOfBranch(
        branchName: String,
        repo: GitRepository,
        consumer: CommitConsumer,
    ): List<GitCommit> {
        if (branchName == referenceBranchName) {
            IRGitUtils.getCommitsOfBranch(repo, consumer)
        } else {
            IRGitUtils.getCommitDifferenceBetweenBranches(branchName, referenceBranchName, repo, consumer)
        }
        return consumer.commits
    }

    /**
     * Consumes all commits and only stops when it reaches the cap of commits you can consume
     */
    class GeneralCommitConsumer : CommitConsumer() {
        override var commits: MutableList<GitCommit> = mutableListOf()

        override fun consume(commit: GitCommit?) {
            if (commitCounter < commitConsumptionCap) {
                commit?.let { commits.add(it) }
                commitCounter++
            }
        }

        fun resetCommits() {
            commits = mutableListOf()
            commitCounter = 0
        }
    }
}
