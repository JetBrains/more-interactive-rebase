package com.jetbrains.interactiveRebase.utils.consumers

import git4idea.GitCommit

/**
 * Consumes all commits and only stops when it reaches the cap of commits you can consume
 */
open class GeneralCommitConsumer : CommitConsumer() {
    override var commits: MutableList<GitCommit> = mutableListOf()

    override fun consume(commit: GitCommit?) {
        commit?.let { commits.add(it) }
    }

    fun resetCommits() {
        commits = mutableListOf()
    }
}
