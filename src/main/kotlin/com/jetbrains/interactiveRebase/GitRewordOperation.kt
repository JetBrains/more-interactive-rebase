package com.jetbrains.interactiveRebase


import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.logger
import com.intellij.vcs.log.VcsCommitMetadata
import com.jetbrains.interactiveRebase.utils.gitUtils.GitAutomaticRebaseEditor
import git4idea.checkin.GitCheckinEnvironment
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.config.GitConfigUtil
import git4idea.config.GitVersionSpecialty
import git4idea.repo.GitRepository
import java.io.File
import java.io.IOException

internal class GitRewordOperation(
        repository: GitRepository,
        private val commit: VcsCommitMetadata,
        private val newMessage: String
) : GitRebaseEditingOperation(repository) {
    init {
        repository.update()
    }

    private val LOG = logger<GitRewordOperation>()

    private val initialHeadPosition = repository.currentRevision!!

    fun execute(): GitCommitEditingOperationResult {
        if (canRewordViaAmend()) {
            val operationResult = rewordViaAmend()
            if (operationResult is GitCommitEditingOperationResult.Complete) {
                return operationResult
            }
        }
        return rewordViaRebase()
    }

    private fun canRewordViaAmend() =
            isLatestCommit() && GitVersionSpecialty.CAN_AMEND_WITHOUT_FILES.existsIn(project)

    private fun isLatestCommit() = commit.id.asString() == initialHeadPosition

    private fun rewordViaRebase(): GitCommitEditingOperationResult {
        val rebaseEditor = GitAutomaticRebaseEditor(project, commit.root,
                entriesEditor = { list -> injectRewordAction(list) },
                plainTextEditor = { editorText -> supplyNewMessage(editorText) })

        return rebase(listOf(commit), rebaseEditor)
    }

    private fun rewordViaAmend(): GitCommitEditingOperationResult {
        val handler = GitLineHandler(project, repository.root, GitCommand.COMMIT)
        val messageFile: File
        try {
            messageFile = GitCheckinEnvironment.createCommitMessageFile(project, repository.root, newMessage)
        }
        catch (e: IOException) {
            LOG.warn("Couldn't create message file", e)
            return GitCommitEditingOperationResult.Incomplete
        }
        handler.addParameters("--amend")
        handler.addParameters("-F")
        handler.addAbsoluteFile(messageFile)
        handler.addParameters("--only") // without any files: to amend only the message
        handler.addParameters("--no-verify") // to prevent unnecessary hooks execution

        val result = Git.getInstance().runCommand(handler)
        repository.update()
        if (result.success()) {
            return GitCommitEditingOperationResult.Complete(repository, commit.parents.first().asString(), initialHeadPosition,
                    repository.currentRevision!!)
        }
        else {
            LOG.warn("Couldn't reword via amend: " + result.errorOutputAsJoinedString)
            return GitCommitEditingOperationResult.Incomplete
        }
    }

    private fun injectRewordAction(list: List<IRGitEntry>): List<IRGitEntry> {
        return list.map { entry ->
            if (entry.action == IRGitEntry.Action.PICK && commit.id.asString().startsWith(entry.commit))
                IRGitEntry(IRGitEntry.Action.REWORD, entry.commit, entry.subject)
            else entry
        }
    }

    private fun supplyNewMessage(editorText: String): String {
        if (editorText.startsWith(commit.fullMessage)) { // there are comments after the proposed message
            return newMessage
        }
        else {
            LOG.error("Unexpected editor content. Charset: ${GitConfigUtil.getCommitEncoding(project, commit.root)}",
                    Attachment("actual.txt", editorText), Attachment("expected.txt", commit.fullMessage))
            throw IllegalStateException("Unexpected editor content")
        }
    }
}