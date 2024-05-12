package com.jetbrains.interactiveRebase.utils.gitUtils

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import com.intellij.vcs.log.VcsCommitMetadata
import com.intellij.vcs.log.VcsShortCommitDetails
import com.intellij.vcs.log.data.VcsLogData
import com.jetbrains.interactiveRebase.*
import com.jetbrains.interactiveRebase.GitRebaseEntryGeneratedUsingLog
import com.jetbrains.interactiveRebase.IRCommitsTable
import com.jetbrains.interactiveRebase.IRGitModel
import git4idea.branch.GitRebaseParams
import git4idea.history.GitHistoryTraverser
import git4idea.i18n.GitBundle
import git4idea.rebase.GitInteractiveRebaseEditorHandler
import git4idea.rebase.GitRebaseEditorHandler
import git4idea.rebase.GitRebaseUtils
import git4idea.repo.GitRepository


class IRGitRebaseUtils (private val project : Project) {
    private val gitUtils = IRGitUtils(project)
    private val repo = gitUtils.getRepository()

    private val LOG = Logger.getInstance("Git.Interactive.Rebase.Using.Log")


    @VisibleForTesting
    //@Throws(CantRebaseUsingLogException::class)
    internal fun getEntriesUsingLog(
            commit: VcsShortCommitDetails,
            logData: VcsLogData
    ): List<GitRebaseEntryGeneratedUsingLog> {
        val traverser: GitHistoryTraverser = IRHistoryTraverser(project, logData)
        val details = mutableListOf<VcsCommitMetadata>()
        try {
            repo?.let {
                traverser.traverse(it.root) { (commitId, parents) ->
                    // commit is not root or merge
                    if (parents.size == 1) {
                        loadMetadataLater(commitId) { metadata ->
                            details.add(metadata)
                        }
                        val hash = traverser.toHash(commitId)
                        hash != commit.id
                    } else {
                        //TODO(actually throw exception)
                        throw Exception("something")
                        //throw CantRebaseUsingLogException(CantRebaseUsingLogException.Reason.MERGE)
                    }
                }
            }
        }
        catch (e: VcsException) {
            //TODO(actually throw exception)
            //throw CantRebaseUsingLogException(CantRebaseUsingLogException.Reason.UNRESOLVED_HASH)
        }

        if (details.last().id != commit.id) {
            //TODO(actually throw exception)
            //throw CantRebaseUsingLogException(CantRebaseUsingLogException.Reason.UNEXPECTED_HASH)
        }

        if (details.any { it.subject.startsWith("fixup!") || it.subject.startsWith("squash!") }) {
            //TODO(actually throw exception)
            //throw CantRebaseUsingLogException(CantRebaseUsingLogException.Reason.FIXUP_SQUASH)
        }

        return details.map { GitRebaseEntryGeneratedUsingLog(it) }.reversed()
    }

    internal fun interactivelyRebaseUsingLog(commit: VcsShortCommitDetails, logData: VcsLogData) {
        val root = repo?.root

        object : Task.Backgroundable(project, GitBundle.message("rebase.progress.indicator.preparing.title")) {
            private var generatedEntries: List<GitRebaseEntryGeneratedUsingLog>? = null

            override fun run(indicator: ProgressIndicator) {
                try {
                    generatedEntries = getEntriesUsingLog(commit, logData)
                }
                catch (e: Exception) {
                    LOG.warn("Couldn't use log for rebasing: ${e.message}")
                }
            }

            override fun onSuccess() {
                generatedEntries?.let { entries ->
                    //val dialog = GitInteractiveRebaseDialog(project, root, entries)
                    //dialog.show()
                    //if (dialog.isOK) {
                    startInteractiveRebase(commit, repo?.let { IRGitEditorHandler(it, entries, IRCommitsTable(entries).rebaseTodoModel) })
                    //}
                } //?: startInteractiveRebase(repository, commit)
            }
        }.queue()
    }

    internal fun startInteractiveRebase(
            commit: VcsShortCommitDetails,
            editorHandler: GitRebaseEditorHandler? = null
    ) {
        object : Task.Backgroundable(project, GitBundle.message("rebase.progress.indicator.title")) {
            override fun run(indicator: ProgressIndicator) {
                val params = repo?.vcs?.let { GitRebaseParams.editCommits(it.version, commit.parents.first().asString(), editorHandler, false) }
                if (params != null) {
                    GitRebaseUtils.rebase(project, listOf(repo), params, indicator)
                }
            }
        }.queue()
    }
//    fun rebase(){
//        //TODO("actually throw some kind of exception")
//        if(repository==null) return
//        val editorHandler: GitRebaseEditorHandler = IRGitEditorHandler(repository, entries, dialog.getModel())
//        val params = GitRebaseParams.editCommits(repository.vcs.version, commit.parents.first().asString(), editorHandler, false)
//        GitRebaseUtils.rebase(project,listOf(repository),params, indicator)
//    }





}

private class IRGitEditorHandler(
        repository: GitRepository,
        private val entriesGeneratedUsingLog: List<GitRebaseEntryGeneratedUsingLog>,
        private val rebaseTodoModel: IRGitModel<GitRebaseEntryGeneratedUsingLog>
)  : GitInteractiveRebaseEditorHandler(repository.project, repository.root)