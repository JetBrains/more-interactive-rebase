package com.jetbrains.interactiveRebase.threads

import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.services.CommitService
import git4idea.GitCommit

/**
 * Quick fix for the EDT problem, could be replaced by coroutines
 */
class CommitInfoThread(private val project: Project) : Thread() {
    private var commits: List<GitCommit> = mutableListOf()

    override fun run() {
        commits = CommitService(project).getCommits()
    }

    fun getCommits(): List<GitCommit> {
        return commits
    }
}
