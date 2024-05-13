package com.jetbrains.interactiveRebase

import com.intellij.dvcs.repo.Repository
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.VcsNotifier
import com.intellij.openapi.vcs.changes.ChangeListManagerImpl
import com.intellij.vcs.log.VcsCommitMetadata
import com.intellij.vcs.log.VcsLogCommitSelection
import com.intellij.vcs.log.VcsLogDataKeys
import com.intellij.vcs.log.data.AbstractDataGetter.Companion.getCommitDetails
import com.intellij.vcs.log.data.LoadingDetails
import com.intellij.vcs.log.data.VcsLogData
import com.intellij.vcs.log.ui.table.size
import git4idea.GitNotificationIdsHolder
import git4idea.GitUtil
import git4idea.i18n.GitBundle
import git4idea.rebase.log.GitCommitEditingActionBase
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener


internal class GitRewordAction : GitSingleCommitEditingAction() {
    override val prohibitRebaseDuringRebasePolicy = ProhibitRebaseDuringRebasePolicy.Prohibit(
            GitBundle.message("rebase.log.action.operation.reword.name")
    )

    override fun checkNotMergeCommit(commitEditingData: SingleCommitEditingData): String? {
        val commit = commitEditingData.selectedCommit
        val repository = commitEditingData.repository
        if (commit.id.asString() == repository.currentRevision) {
            // allow amending merge commit
            return null
        }

        return super.checkNotMergeCommit(commitEditingData)
    }

    fun actionSomething(e: AnActionEvent) {
        val commitEditingRequirements = (createCommitEditingData(e) as CommitEditingDataCreationResult.Created<*>).data
        val something = commitEditingRequirements as SingleCommitEditingData
        val description = lastCheckCommitsEditingAvailability(something)

        if (description != null) {
            Messages.showErrorDialog(
                    commitEditingRequirements.project,
                    description,
                    getFailureTitle()
            )
            return
        }
        actionPerformedAfterChecks(something)
    }

    override fun actionPerformedAfterChecks(commitEditingData: SingleCommitEditingData) {
        val details = getOrLoadDetails(commitEditingData.project, commitEditingData.logData, commitEditingData.selection)
        val commit = details.first()
//        val dialog = GitNewCommitMessageActionDialog(
//                commitEditingData,
//                originMessage = commit.fullMessage,
//                title = GitBundle.message("rebase.log.reword.dialog.title"),
//                dialogLabel = GitBundle.message(
//                        "rebase.log.reword.dialog.description.label",
//                        commit.id.toShortString(),
//                        VcsUserUtil.getShortPresentation(commit.author)
//                )
//        )
        //dialog.show { newMessage ->
            rewordInBackground(commitEditingData.project, commit, commitEditingData.repository, "newMessage")
        //}
    }


    private fun createCommitEditingData(e: AnActionEvent) : CommitEditingDataCreationResult<SingleCommitEditingData> {
        val project = e.project
        val selection = e.getData(VcsLogDataKeys.VCS_LOG_COMMIT_SELECTION)
        val logDataProvider = e.getData(VcsLogDataKeys.VCS_LOG_DATA_PROVIDER) as VcsLogData?

        if (project == null || selection == null || logDataProvider == null) {
            return CommitEditingDataCreationResult.Prohibited()
        }

        val commitList = selection.commits.takeIf { it.isNotEmpty() } ?: return CommitEditingDataCreationResult.Prohibited()
        val repositoryManager = GitUtil.getRepositoryManager(project)

        val root = commitList.map { it.root }.distinct().singleOrNull() ?: return CommitEditingDataCreationResult.Prohibited(
                GitBundle.message("rebase.log.multiple.commit.editing.action.disabled.multiple.repository.description", commitList.size)
        )
        val repository = repositoryManager.getRepositoryForRootQuick(root) ?: return CommitEditingDataCreationResult.Prohibited()
        if (repositoryManager.isExternal(repository)) {
            return CommitEditingDataCreationResult.Prohibited(
                    GitBundle.message("rebase.log.multiple.commit.editing.action.disabled.external.repository.description", commitList.size)
            )
        }

        return createCommitEditingData(repository, selection, logDataProvider)
    }

    override fun getFailureTitle(): String = GitBundle.message("rebase.log.reword.action.failure.title")

    private fun rewordInBackground(project: Project, commit: VcsCommitMetadata, repository: GitRepository, newMessage: String) {
        object : Task.Backgroundable(project, GitBundle.message("rebase.log.reword.action.progress.indicator.title")) {
            override fun run(indicator: ProgressIndicator) {
                val operationResult = GitRewordOperation(repository, commit, newMessage).execute()
                if (operationResult is GitCommitEditingOperationResult.Complete) {
                    operationResult.notifySuccess(
                            GitBundle.message("rebase.log.reword.action.notification.successful.title"),
                            null,
                            GitBundle.message("rebase.log.reword.action.progress.indicator.undo.title"),
                            GitBundle.message("rebase.log.reword.action.notification.undo.not.allowed.title"),
                            GitBundle.message("rebase.log.reword.action.notification.undo.failed.title")
                    )
                    ChangeListManagerImpl.getInstanceImpl(project).replaceCommitMessage(commit.fullMessage, newMessage)
                }
            }
        }.queue()
    }

    override fun getProhibitedStateMessage(commitEditingData: SingleCommitEditingData, operation: String): String? {
        if (commitEditingData.repository.state == Repository.State.REBASING && commitEditingData.isHeadCommit) {
            return null
        }
        return super.getProhibitedStateMessage(commitEditingData, operation)
    }

    private val LOG = Logger.getInstance("Git.Rebase.Log.Action.CommitDetailsLoader")

    internal fun getOrLoadDetails(project: Project, data: VcsLogData, selection: VcsLogCommitSelection): List<VcsCommitMetadata> {
        val cachedCommits = ArrayList(selection.cachedMetadata)
        if (cachedCommits.none { it is LoadingDetails }) return cachedCommits

        return loadDetails(project, data, selection)
    }

    private fun loadDetails(project: Project, data: VcsLogData, selection: VcsLogCommitSelection): List<VcsCommitMetadata> {
        try {
            val loadedDetails = ProgressManager.getInstance().runProcessWithProgressSynchronously(
                    ThrowableComputable<List<VcsCommitMetadata>, VcsException> {
                        return@ThrowableComputable data.miniDetailsGetter.getCommitDetails(selection.ids)
                    },
                    GitBundle.message("rebase.log.action.progress.indicator.loading.commit.message.title", selection.size),
                    true,
                    project
            )
            if (loadedDetails.size != selection.size) throw LoadCommitDetailsException()
            return loadedDetails
        }
        catch (e: VcsException) {
            val error = GitBundle.message("rebase.log.action.loading.commit.message.failed.message", selection.size)
            LOG.warn(error, e)
            val notification = VcsNotifier.STANDARD_NOTIFICATION
                    .createNotification(error, NotificationType.ERROR)
                    .setDisplayId(GitNotificationIdsHolder.COULD_NOT_LOAD_CHANGES_OF_COMMIT_LOG)
            VcsNotifier.getInstance(project).notify(notification)
            throw LoadCommitDetailsException()
        }
    }

    internal class LoadCommitDetailsException : Exception()

    internal fun  GitCommitEditingOperationResult.Complete.notifySuccess(
            @NlsContexts.NotificationTitle title: String,
            @NlsContexts.NotificationContent content: String?,
            @NlsContexts.ProgressTitle undoProgressTitle: String,
            @NlsContexts.ProgressTitle undoImpossibleTitle: String,
            @NlsContexts.ProgressTitle undoErrorTitle: String
    ) {
        val project = repository.project
        val notification = if (content.isNullOrEmpty()) VcsNotifier.STANDARD_NOTIFICATION.createNotification(title, NotificationType.INFORMATION)
        else VcsNotifier.STANDARD_NOTIFICATION.createNotification(title, content, NotificationType.INFORMATION)
        notification.setDisplayId(GitNotificationIdsHolder.COMMIT_EDIT_SUCCESS)
        notification.addAction(NotificationAction.createSimple(
                GitBundle.messagePointer("action.NotificationAction.GitRewordOperation.text.undo"),
                Runnable {
                    undoInBackground(project, undoProgressTitle, undoImpossibleTitle, undoErrorTitle, this@notifySuccess) { notification.expire() }
                }
        ))

        val connection = project.messageBus.connect()
        notification.whenExpired { connection.disconnect() }
        connection.subscribe(GitRepository.GIT_REPO_CHANGE, GitRepositoryChangeListener {
            if (it == repository) {
                BackgroundTaskUtil.executeOnPooledThread(repository, Runnable {
                    if (checkUndoPossibility() !== GitCommitEditingOperationResult.Complete.UndoPossibility.Possible) {
                        notification.expire()
                    }
                })
            }
        })

        VcsNotifier.getInstance(project).notify(notification)
    }

    internal fun GitCommitEditingOperationResult.Complete.UndoResult.Error.notifyUndoError(project: Project, @NlsContexts.NotificationTitle title: String) {
        VcsNotifier.getInstance(project).notifyError(GitNotificationIdsHolder.REBASE_COMMIT_EDIT_UNDO_ERROR, title, errorHtml)
    }

    internal fun GitCommitEditingOperationResult.Complete.UndoPossibility.Impossible.notifyUndoImpossible(project: Project, @NlsContexts.NotificationTitle title: String) {
        val notifier = VcsNotifier.getInstance(project)
        when (this) {
            GitCommitEditingOperationResult.Complete.UndoPossibility.Impossible.HeadMoved -> {
                notifier.notifyError(GitNotificationIdsHolder.REBASE_COMMIT_EDIT_UNDO_ERROR_REPO_CHANGES,
                        title,
                        GitBundle.message("rebase.log.reword.action.notification.undo.not.allowed.repository.changed.message"))
            }
            is GitCommitEditingOperationResult.Complete.UndoPossibility.Impossible.PushedToProtectedBranch -> {
                notifier.notifyError(GitNotificationIdsHolder.REBASE_COMMIT_EDIT_UNDO_ERROR_PROTECTED_BRANCH,
                        title,
                        GitBundle.message("rebase.log.undo.impossible.pushed.to.protected.branch.notification.text", branch))
            }
        }
    }

    private fun undoInBackground(
            project: Project,
            @NlsContexts.ProgressTitle undoProgressTitle: String,
            @NlsContexts.ProgressTitle undoImpossibleTitle: String,
            @NlsContexts.ProgressTitle undoErrorTitle: String,
            result: GitCommitEditingOperationResult.Complete,
            expireUndoAction: () -> Unit
    ) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, undoProgressTitle) {
            override fun run(indicator: ProgressIndicator) {
                val possibility = result.checkUndoPossibility()
                if (possibility is GitCommitEditingOperationResult.Complete.UndoPossibility.Impossible) {
                    possibility.notifyUndoImpossible(project, undoImpossibleTitle)
                    expireUndoAction()
                    return
                }
                when (val undoResult = result.undo()) {
                    is GitCommitEditingOperationResult.Complete.UndoResult.Error -> undoResult.notifyUndoError(project, undoErrorTitle)
                    is GitCommitEditingOperationResult.Complete.UndoResult.Success -> expireUndoAction()
                }
            }
        })
    }



}