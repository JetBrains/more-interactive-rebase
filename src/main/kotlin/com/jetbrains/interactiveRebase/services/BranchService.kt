package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.interactiveRebase.exceptions.IRInaccessibleException
import com.jetbrains.interactiveRebase.utils.gitUtils.IRGitUtils
import git4idea.commands.GitCommand
import git4idea.commands.GitCommandResult
import git4idea.commands.GitLineHandler

@Service(Service.Level.PROJECT)
class BranchService(private val project: Project, private val gitUtils: IRGitUtils) {
    constructor(project: Project) : this(project, IRGitUtils(project))

    /**
     * Gets the primary branch name in the project. Return main or master
     */
    fun getDefaultReferenceBranchName(): String? {
        val params = listOf("-l", "main", "master", "--format=%(refname:short)")
        val result = executeGitBranchCommand(params)
        return validateReferenceBranchOutput(result.getOutputOrThrow())
    }

    /**
     * Checks the terminal output to determine if the reference branch is main or master. Returns null
     * if there is no main or master or there is both
     */
    fun validateReferenceBranchOutput(result: String): String? {
        if (result.isEmpty() || result.contains("master") && result.contains("main")) {
            return null
        }
        return result.trimMargin("*").trim()
    }

    /**
     * Checks if a branch has already been merged to the reference branch
     */
    fun isBranchMerged(branchName: String): Boolean {
        val params = listOf("--merged")
        val result = executeGitBranchCommand(params)
        val branches: List<String> = result.getOutputOrThrow().split("\n")

        val mergedBranches: List<String> = branches.map { it.trimMargin("*").trim() }

        return mergedBranches.contains(branchName)
    }

    /**
     * Executes git branch command with the given parameters
     */
    fun executeGitBranchCommand(params: List<String>): GitCommandResult {
        val branchCommand: GitCommand = GitCommand.BRANCH
        val root: VirtualFile = gitUtils.getRoot() ?: throw IRInaccessibleException("Project root cannot be found")
        val lineHandler = GitLineHandler(project, root, branchCommand)
        lineHandler.addParameters(params)
        return gitUtils.runCommand(lineHandler)
    }
}
