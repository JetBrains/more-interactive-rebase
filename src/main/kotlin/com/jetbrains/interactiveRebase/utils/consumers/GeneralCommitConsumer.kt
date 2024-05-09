package com.jetbrains.interactiveRebase.utils.consumers

import git4idea.GitCommit

/**
 * Consumes all commits and only stops when it reaches the cap of commits you can consume
 */
open class GeneralCommitConsumer : CommitConsumer() {
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
