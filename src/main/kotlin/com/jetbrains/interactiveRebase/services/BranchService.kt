package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.interactiveRebase.exceptions.IRInaccessibleException
import com.jetbrains.interactiveRebase.utils.IRGitUtils
import git4idea.commands.GitCommand
import git4idea.commands.GitCommandResult
import git4idea.commands.GitLineHandler

@Service(Service.Level.PROJECT)
class BranchService(private val project: Project, private val gitUtils: IRGitUtils) {
    constructor(project: Project) : this(project, IRGitUtils(project))

    /**
     * Gets the primary branch name in the project. Return main or master
     */
    fun getDefaultReferenceBranchName(): String {
        val branchCommand: GitCommand = GitCommand.BRANCH
        val root: VirtualFile = gitUtils.getRoot() ?: throw IRInaccessibleException("Project root cannot be found")
        val params = listOf("-l", "main", "master", "--format=%(refname:short)")
        val lineHandler = GitLineHandler(project, root, branchCommand)
        lineHandler.addParameters(params)
        val result: GitCommandResult = gitUtils.runCommand(lineHandler)

        return result.getOutputOrThrow().trimMargin("*").trim()
    }

    /**
     * Checks if a branch has already been merged to the reference branch
     */
    fun isBranchMerged(branchName: String): Boolean {
        val branchCommand: GitCommand = GitCommand.BRANCH
        val root: VirtualFile = gitUtils.getRoot() ?: throw IRInaccessibleException("Project root cannot be found")
        val params = listOf("--merged")
        val lineHandler = GitLineHandler(project, root, branchCommand)
        lineHandler.addParameters(params)
        val result: GitCommandResult = gitUtils.runCommand(lineHandler)
        val branches: List<String> = result.getOutputOrThrow().split("\n")

        val mergedBranches: List<String> = branches.map { it.trimMargin("*").trim() }

        return mergedBranches.contains(branchName)
    }
}
