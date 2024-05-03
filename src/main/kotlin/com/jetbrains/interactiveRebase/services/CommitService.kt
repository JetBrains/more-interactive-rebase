package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.jetbrains.interactiveRebase.CommitConsumer
import git4idea.GitCommit
import git4idea.GitUtil
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepository

@Service(Service.Level.PROJECT)
class CommitService(private val project: Project) {
    private var referenceBranchName = "master" // TODO this should be configurable, setter already exists

    /**
     * Finds the current branch and returns all the commits that are on the current branch and not on the reference branch.
     * If the reference branch is the current branch only the maximum amount of commits are displayed
     */
    fun getCommits() : List<GitCommit> {
        val repo = GitUtil.getRepositoryManager(project).getRepositoryForRoot(project.guessProjectDir())

        if (repo == null) thisLogger().warn("null repository")
        var displayableCommits = emptyList<GitCommit>()

        val branchName = repo?.currentBranchName
        if (branchName != null) {
            displayableCommits =  getDisplayableCommitsOfBranch(branchName, repo)
        } else {
            // TODO get better handling of this
            thisLogger().warn("branch name is null")
        }
        return displayableCommits
    }

//    /**
//     * Gets the commits on a given branch, given a consumer
//     */
//    fun getCommitsOfBranch(branchName: String, consumer: CommitConsumer, repo : GitRepository) : List<GitCommit> {
//        GitHistoryUtils.loadDetails(project, repo.root, consumer)
//        consumer.commits.forEach{commit -> println("commit: " + commit.subject)}
//        return consumer.commits
//    }

    fun getDisplayableCommitsOfBranch(branchName: String, repo : GitRepository) : List<GitCommit> {
        val consumer = GeneralCommitConsumer()
        GitHistoryUtils.loadDetails(project, repo.root, consumer, branchName, "--not", referenceBranchName)
        println("in branch $branchName and not $referenceBranchName")
        return consumer.commits

//        // get all commits of reference branch to compare
//        // TODO instead of getting all commits, a cap can be added for optimization
//        val referenceConsumer = GeneralCommitConsumer()
//        val referenceCommits : List<GitCommit> =  getCommitsOfBranch(referenceBranchName, referenceConsumer, repo)
//
//        // use the reference branch's commits to only consume commits that are not in the reference branch
//        val displayableConsumer = DisplayableCommitConsumer(referenceCommits.toSet())
//        return displayableConsumer.commits
    }

    /**
     * Consumes all commits and only stops when it reaches the cap of commits you can consume
     */
    class GeneralCommitConsumer : CommitConsumer() {
        override var commits: MutableList<GitCommit> = mutableListOf()
        override fun consume(commit: GitCommit?) {
            if (commitCounter <= commitConsumptionCap) {
                commit?.let { commits.add(it) }
                commitCounter++
                println("in consumer $commit with counter $commitCounter")
            }
        }
    }

//    /**
//     *
//     */
//    class DisplayableCommitConsumer(private val referenceCommits : Set<GitCommit>) : CommitConsumer() {
//        override var commits: MutableList<GitCommit> = mutableListOf()
//        override fun consume(commit: GitCommit?) {
//            if (commitCounter <= commitConsumptionCap && !referenceCommits.contains(commit) ) {
//                commit?.let { commits.add(it) }
//                commitCounter++
//            }
//        }
//    }



}
