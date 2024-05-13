package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.committed.CommittedChangesTreeBrowser
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.vcs.log.VcsCommitMetadata
import com.intellij.vcs.log.ui.details.FullCommitDetailsListPanel
import com.jetbrains.interactiveRebase.utils.IRGitUtils
import git4idea.GitDisposable
import git4idea.history.GitCommitRequirements
import git4idea.history.GitLogUtil

/*
Creates the panel that displays all the files
changed by a commit as well as all the commit info
 */

class CommitInfoPanel(private val project: Project) : FullCommitDetailsListPanel(
    project,
    GitDisposable.getInstance(project),
    ModalityState.current(),
) {
    @RequiresBackgroundThread
    @Throws(VcsException::class)
    public override fun loadChanges(commits: List<VcsCommitMetadata>): List<Change> {
        val changes = mutableListOf<Change>()
        IRGitUtils(project).getRepository()?.let {
            GitLogUtil.readFullDetailsForHashes(
                project,
                it.root,
                commits.map { it.id.asString() },
                GitCommitRequirements.DEFAULT,
            ) { gitCommit ->
                changes.addAll(gitCommit.changes)
                println(gitCommit.changes)
            }
        }
        return CommittedChangesTreeBrowser.zipChanges(changes)
    }
}
