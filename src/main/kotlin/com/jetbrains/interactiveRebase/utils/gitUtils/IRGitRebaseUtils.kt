package com.jetbrains.interactiveRebase.utils.gitUtils

import com.intellij.application.options.CodeStyle.LOG
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.vcs.log.VcsShortCommitDetails
import com.intellij.vcs.log.impl.VcsCommitMetadataImpl
import com.jetbrains.interactiveRebase.*
import com.jetbrains.interactiveRebase.GitRebaseEntryGeneratedUsingLog
import com.jetbrains.interactiveRebase.IRCommitsTable
import com.jetbrains.interactiveRebase.IRGitModel
import git4idea.GitUtil
import git4idea.branch.GitRebaseParams
import git4idea.config.GitConfigUtil
import git4idea.i18n.GitBundle
import git4idea.rebase.*
import git4idea.repo.GitRepository
import java.io.File


class IRGitRebaseUtils (private val project : Project) {
    private val gitUtils = IRGitUtils(project)

    private val repo = GitUtil.getRepositoryManager(project).getRepositoryForRootQuick(project.guessProjectDir())



    private val LOG = Logger.getInstance("Git.Interactive.Rebase.Using.Log")


//    @VisibleForTesting
//    //@Throws(CantRebaseUsingLogException::class)
//    internal fun getEntriesUsingLog(
//            commit: VcsShortCommitDetails,
//            logData: VcsLogData
//    ): List<GitRebaseEntryGeneratedUsingLog> {
//        val traverser: GitHistoryTraverser = IRHistoryTraverser(project, logData)
//        val details = mutableListOf<VcsCommitMetadata>()
//        try {
//            repo?.let {
//                traverser.traverse(it.root) { (commitId, parents) ->
//                    // commit is not root or merge
//                    if (parents.size == 1) {
//                        loadMetadataLater(commitId) { metadata ->
//                            details.add(metadata)
//                        }
//                        val hash = traverser.toHash(commitId)
//                        hash != commit.id
//                    } else {
//                        //TODO(actually throw exception)
//                        throw Exception("something")
//                        //throw CantRebaseUsingLogException(CantRebaseUsingLogException.Reason.MERGE)
//                    }
//                }
//            }
//        }
//        catch (e: VcsException) {
//            //TODO(actually throw exception)
//            //throw CantRebaseUsingLogException(CantRebaseUsingLogException.Reason.UNRESOLVED_HASH)
//        }
//
//        if (details.last().id != commit.id) {
//            //TODO(actually throw exception)
//            //throw CantRebaseUsingLogException(CantRebaseUsingLogException.Reason.UNEXPECTED_HASH)
//        }
//
//        if (details.any { it.subject.startsWith("fixup!") || it.subject.startsWith("squash!") }) {
//            //TODO(actually throw exception)
//            //throw CantRebaseUsingLogException(CantRebaseUsingLogException.Reason.FIXUP_SQUASH)
//        }
//
//        return details.map { GitRebaseEntryGeneratedUsingLog(it) }.reversed()
//    }

    internal fun interactivelyRebaseUsingLog(commit: VcsCommitMetadataImpl) {
        //val root = repo?.root

        object : Task.Backgroundable(project, GitBundle.message("rebase.progress.indicator.preparing.title")) {
            private var generatedEntries: List<GitRebaseEntryGeneratedUsingLog>? = null

            override fun run(indicator: ProgressIndicator) {
                try {

                    generatedEntries = listOf(GitRebaseEntryGeneratedUsingLog(commit))
                //getEntriesUsingLog(commit, logData)
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
)  : IREditorHandler(repository.project, repository.root) {
    private var rebaseFailed = false

    override fun collectNewEntries(entries: List<IRGitEntry>): List<IRGitEntry>? {
        if (rebaseFailed) {
            return super.collectNewEntries(entries)
        }
        entriesGeneratedUsingLog.forEachIndexed { i, generatedEntry ->
            val realEntry = entries[i]
            if (!generatedEntry.equalsWithReal(realEntry)) {
                myRebaseEditorShown = false
                rebaseFailed = true
                LOG.error(
                        "Incorrect git-rebase-todo file was generated",
                        Attachment("generated.txt", entriesGeneratedUsingLog.joinToString("\n")),
                        Attachment("expected.txt", entries.joinToString("\n"))
                )
                throw VcsException(GitBundle.message("rebase.using.log.couldnt.start.error"))
            }
        }
        processModel(rebaseTodoModel)
        return rebaseTodoModel.convertToEntries()
    }

}

internal class GitAutomaticRebaseEditor(private val project: Project,
                                        private val root: VirtualFile,
                                        private val entriesEditor: (List<IRGitEntry>) -> List<IRGitEntry>,
                                        private val plainTextEditor: (String) -> String
) : GitInteractiveRebaseEditorHandler(project, root) {
    companion object {
        private val LOG = logger<GitAutomaticRebaseEditor>()
    }

    override fun editCommits(file: File): Int {
        try {
            if (!myRebaseEditorShown) {
                myRebaseEditorShown = true

                val rebaseFile = IRGitRebaseFile(project, root, file)
                val entries = rebaseFile.load()
                rebaseFile.save(entriesEditor(entries))
            }
            else {
                val encoding = GitConfigUtil.getCommitEncoding(project, root)
                val originalMessage = FileUtil.loadFile(file, encoding)
                val modifiedMessage = plainTextEditor(originalMessage)
                FileUtil.writeToFile(file, modifiedMessage.toByteArray(charset(encoding)))
            }
            return 0
        }
        catch (ex: Exception) {
            LOG.error("Editor failed: ", ex)
            return ERROR_EXIT_CODE
        }
    }
}