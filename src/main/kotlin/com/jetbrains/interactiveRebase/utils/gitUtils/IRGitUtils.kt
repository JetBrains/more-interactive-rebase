package com.jetbrains.interactiveRebase.utils.gitUtils

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Consumer
import com.jetbrains.interactiveRebase.exceptions.IRInaccessibleException
import git4idea.GitCommit
import git4idea.GitUtil
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitCommandResult
import git4idea.commands.GitLineHandler
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepository

/**
 * Isolated interaction with static git utility methods
 */
class IRGitUtils(private val project: Project) {
    /**
     * Gets the GitRepository given the project
     */
    fun getRepository(): GitRepository {
        return GitUtil.getRepositoryManager(
            project,
        ).getRepositoryForRoot(getRoot()) ?: throw IRInaccessibleException("Repository cannot be accessed")
    }

    /**
     *  Gets the root of project by guessing directory
     */
    fun getRoot(): VirtualFile? {
        return project.guessProjectDir()
    }

    /**
     * Gets the commits that are on currentBranch but not on reference branch, consuming them with the specified consumer
     */
    fun getCommitDifferenceBetweenBranches(
        currentBranch: String,
        referenceBranch: String,
        repo: GitRepository,
        consumer: Consumer<GitCommit>,
    ) {
        GitHistoryUtils.loadDetails(project, repo.root, consumer, currentBranch, "--not", referenceBranch)
    }

    /**
     * Given a hash, loads the commit as a GitCommit object
     */
    fun collectACommit(
        repo: GitRepository,
        hash: String,
        consumer: Consumer<GitCommit>,
    ) {
        return GitHistoryUtils.loadDetails(project, repo.root, consumer, "-1", hash)
    }

    /**
     * Gets the commits of a branch regardless of a reference branch. Consumed by the given consumer
     */
    fun getCommitsOfBranch(
        repo: GitRepository,
        consumer: Consumer<GitCommit>,
        branchName: String,
    ) {
        GitHistoryUtils.loadDetails(project, repo.root, consumer, branchName)
    }

    /**
     * Runs the specified git command
     */
    fun runCommand(lineHandler: GitLineHandler): GitCommandResult {
        return Git.getInstance().runCommand(lineHandler)
    }

    fun retrieveBranchName(): String {
        val branchCommand: GitCommand = GitCommand.REV_PARSE
        val root: VirtualFile = getRoot() ?: throw IRInaccessibleException("Project root cannot be found")
        val lineHandler = GitLineHandler(project, root, branchCommand)
        val params = listOf("--abbrev-ref", "HEAD")
        lineHandler.addParameters(params)
        val output: GitCommandResult = runCommand(lineHandler)
        return output.getOutputOrThrow()
    }
}
