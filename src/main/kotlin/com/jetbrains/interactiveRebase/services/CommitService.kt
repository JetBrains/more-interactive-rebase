package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.jetbrains.interactiveRebase.CommitConsumer
import com.jetbrains.interactiveRebase.exceptions.IRebaseInaccessibleException
import com.jetbrains.interactiveRebase.utils.IRGitUtils
import git4idea.GitCommit
import git4idea.GitUtil
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepository

@Service(Service.Level.PROJECT)
class CommitService(private val project: Project) {
    private var referenceBranchName = "origin/master" // TODO this should be configurable, setter already exists
    private val IRGitUtils = IRGitUtils(project)
    /**
     * Finds the current branch and returns all the commits that are on the current branch and not on the reference branch.
     * If the reference branch is the current branch only the maximum amount of commits are displayed
     */
    fun getCommits() : List<GitCommit> {
        val repo = IRGitUtils.getRepository()
            ?: throw IRebaseInaccessibleException("GitRepository cannot be accessed")

        val branchName = repo.currentBranchName ?: throw IRebaseInaccessibleException("cannot access current branch")
        return getDisplayableCommitsOfBranch(branchName, repo)
    }


    /**
     * Setter for the reference branch parameter
     */
    fun setReferenceBranch(newReference: String) {
        referenceBranchName = newReference
    }

    /**
     * Gets the commits in the given branch that are not on the reference branch, caps them to the maximum size at the consumer.
     */
    private fun getDisplayableCommitsOfBranch(branchName: String, repo : GitRepository) : List<GitCommit> {
        val consumer = GeneralCommitConsumer()
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
