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
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitCommandResult
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository

@Service(Service.Level.PROJECT)
class CommitService(private val project: Project, private val gitUtils: IRGitUtils) {
    var referenceBranchName = "main"
    constructor(project: Project) : this(project, IRGitUtils(project))

    /**
     * Finds the current branch and returns all the commits that are on the current branch and not on the reference branch.
     * If the reference branch is the current branch only the maximum amount of commits are displayed
     * Reference branch is dynamically set to master or main according to the repo
     */
    fun getCommits(): List<GitCommit> {
        referenceBranchName = getDefaultReferenceBranchName()
        val repo =
            gitUtils.getRepository()
                ?: throw IRInaccessibleException("GitRepository cannot be accessed")

        val branchName = repo.currentBranchName ?: throw IRInaccessibleException("cannot access current branch")
        val consumer = GeneralCommitConsumer()
        return getDisplayableCommitsOfBranch(branchName, repo, consumer)
    }

    fun getDefaultReferenceBranchName() : String {
        val branchCommand : GitCommand = GitCommand.BRANCH
        val root : VirtualFile = project.guessProjectDir() ?: throw IRInaccessibleException("project root cannot be found")
        val params  = listOf("-l", "main", "master", "--format=%(refname:short)")
        val lineHandler = GitLineHandler(project, root, branchCommand)
        lineHandler.addParameters(params)
        val result : GitCommandResult = gitUtils.runCommand(lineHandler)

        return result.getOutputOrThrow()
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
