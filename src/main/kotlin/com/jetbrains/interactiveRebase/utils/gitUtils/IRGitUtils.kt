package com.jetbrains.interactiveRebase.utils.gitUtils

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Consumer
import git4idea.GitCommit
import git4idea.GitUtil
import git4idea.commands.Git
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
    fun getRepository(): GitRepository? {
        return GitUtil.getRepositoryManager(project).getRepositoryForRoot(getRoot())
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
     * Gets the commits of a branch regardless of a reference branch. Consumed by the given consumer
     */
    fun getCommitsOfBranch(
        repo: GitRepository,
        consumer: Consumer<GitCommit>,
    ) {
        GitHistoryUtils.loadDetails(project, repo.root, consumer)
    }

    /**
     * Runs the specified git command
     */
    fun runCommand(lineHandler: GitLineHandler): GitCommandResult {
        return Git.getInstance().runCommand(lineHandler)
    }
}