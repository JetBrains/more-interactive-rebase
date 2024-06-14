package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.exceptions.IRInaccessibleException
import com.jetbrains.interactiveRebase.utils.consumers.CommitConsumer
import com.jetbrains.interactiveRebase.utils.consumers.GeneralCommitConsumer
import com.jetbrains.interactiveRebase.utils.gitUtils.IRGitUtils
import git4idea.GitCommit
import git4idea.commands.GitCommand
import git4idea.commands.GitCommandResult
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository

@Service(Service.Level.PROJECT)
class CommitService(private val project: Project) {
    /**
     * Usually the primary branch master or main, can be configured when adding a new branch
     */
    var referenceBranchName: String = ""
    private var gitUtils: IRGitUtils = IRGitUtils(project)
    internal var branchSer = project.service<BranchService>()

    /**
     * Secondary constructor for testing
     */
    constructor(project: Project, gitUtils: IRGitUtils, branchSer: BranchService) : this(project) {
        this.gitUtils = gitUtils
        this.branchSer = branchSer
    }

    /**
     * Finds the current branch and returns all the commits that are on the current branch and not on the reference branch.
     * If the reference branch is the current branch only the maximum amount of commits are displayed
     * Reference branch is dynamically set to master or main according to the repo, if none or both exist,
     * we disregard the reference branch
     */
    fun getCommits(branchName: String): List<GitCommit> {
        var repo: GitRepository?
        try {
            repo = gitUtils.getRepository()
        }catch(_:IRInaccessibleException){
            return getCommits(branchName)
        }
        val consumer = GeneralCommitConsumer()

        // if the reference branch is not set for the branch
        if (referenceBranchName.isEmpty()) {
            referenceBranchName = branchSer.getDefaultReferenceBranchName() ?: branchName
        }
        return getDisplayableCommitsOfBranch(branchName, repo, consumer)



    }

    /**
     * Gets the commits that are on wanted branch but not on reference branch
     */
    fun getCommitsWithReference(
        wantedBranch: String,
        referenceBranch: String,
    ): List<GitCommit> {
        val initial = this.referenceBranchName
        this.referenceBranchName = referenceBranch
        val commits = getCommits(wantedBranch)
        this.referenceBranchName = initial
        return commits
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
        if (referenceBranchName == branchName) {
            gitUtils.getCommitsOfBranch(repo, consumer, branchName)
        } else {
            gitUtils.getCommitDifferenceBetweenBranches(branchName, referenceBranchName, repo, consumer)
//            handleMergedBranch(consumer, branchName, repo)
        }
        return consumer.commits
    }

    /**
     * Handles the case where there are no commits to be displayed since the branch is merged.
     * Gets the commits on the branch until the cap. These commits are also on the reference branch
     * **/
    fun handleMergedBranch(
        consumer: CommitConsumer,
        branchName: String,
        repo: GitRepository,
    ) {
        if (consumer.commits.isEmpty() && branchSer.isBranchMerged(branchName)) {
            gitUtils.getCommitsOfBranch(repo, consumer, branchName)
        }
    }

    /**
     * Maps GitCommits to CommitInfo objects
     */
    fun getCommitInfoForBranch(commits: List<GitCommit>): List<CommitInfo> {
        return commits.map { commit ->
            CommitInfo(commit, project, mutableListOf())
        }
    }

    /**
     * Given a hash, returns the corresponding GitCommit, used to retrieve merging commit
     */
    fun turnHashToCommit(hash: String): GitCommit {
        val consumer = GeneralCommitConsumer()
        gitUtils.collectACommit(gitUtils.getRepository(), hash, consumer)
        if (consumer.commits.isEmpty()) {
            throw IRInaccessibleException("Commit hash not found")
        }
        return consumer.commits[0]
    }

    /**
     * Gets branch name from utils
     */
    fun getBranchName(): String {
        val branchCommand: GitCommand = GitCommand.REV_PARSE
        val root: VirtualFile = gitUtils.getRoot() ?: throw IRInaccessibleException("Project root cannot be found")
        val lineHandler = GitLineHandler(project, root, branchCommand)
        val params = listOf("--abbrev-ref", "HEAD")
        lineHandler.addParameters(params)
        val output: GitCommandResult = gitUtils.runCommand(lineHandler)
        return output.getOutputOrThrow()
    }
}
