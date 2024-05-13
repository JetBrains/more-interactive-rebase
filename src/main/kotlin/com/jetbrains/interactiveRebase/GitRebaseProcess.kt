package com.jetbrains.interactiveRebase

import com.google.common.annotations.VisibleForTesting
import com.intellij.CommonBundle
import com.intellij.application.options.CodeStyle.LOG
import com.intellij.dvcs.DvcsUtil
import com.intellij.dvcs.repo.Repository
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vcs.VcsNotifier
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.VcsDirtyScopeManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ExceptionUtil
import com.intellij.util.ObjectUtils
import com.intellij.util.ThreeState
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.progress.StepsProgressIndicator
import com.intellij.vcs.log.TimedVcsCommit
import com.jetbrains.interactiveRebase.GitRebaseStatus
import com.jetbrains.interactiveRebase.MySpec
import git4idea.*
import git4idea.GitActivity.Rebase
import git4idea.commands.*
import git4idea.config.GitSaveChangesPolicy
import git4idea.history.GitHistoryUtils
import git4idea.i18n.GitBundle
import git4idea.merge.GitConflictResolver
import git4idea.rebase.GitRebaseResumeMode
import git4idea.rebase.GitRebaseUtils
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import git4idea.stash.GitChangesSaver
import git4idea.util.GitFreezingProcess
import git4idea.util.GitUntrackedFilesHelper
import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.UnknownNullability
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern


class GitRebaseProcess(val myProject : Project, val myRebaseSpec : MySpec, val myCustomMode : GitRebaseResumeMode?) {

    private val ABORT_ACTION = NotificationAction.createSimpleExpiring(
            GitBundle.message("rebase.notification.action.abort.text"),
            GitActionIdsHolder.Id.ABORT.id) { abort() }
    private val CONTINUE_ACTION = NotificationAction.createSimpleExpiring(
            GitBundle.message("rebase.notification.action.continue.text"),
            GitActionIdsHolder.Id.CONTINUE.id
    ) { retry(GitBundle.message("rebase.progress.indicator.continue.title")) }
    private val RETRY_ACTION = NotificationAction.createSimpleExpiring(
            GitBundle.message("rebase.notification.action.retry.text"),
            GitActionIdsHolder.Id.RETRY.id
    ) { retry(GitBundle.message("rebase.progress.indicator.retry.title")) }
    private val VIEW_STASH_ACTION: NotificationAction
    private val myGit: Git
    private val myChangeListManager: ChangeListManager
    private val myNotifier: VcsNotifier
    private val myRepositoryManager: GitRepositoryManager
    private val mySaver: GitChangesSaver
    private val myProgressManager: ProgressManager
    private val myDirtyScopeManager: VcsDirtyScopeManager

    init {
        mySaver = myRebaseSpec.saver
        myGit = Git.getInstance()
        myChangeListManager = ChangeListManager.getInstance(myProject)
        myNotifier = VcsNotifier.getInstance(myProject)
        myRepositoryManager = GitUtil.getRepositoryManager(myProject)
        myProgressManager = ProgressManager.getInstance()
        myDirtyScopeManager = VcsDirtyScopeManager.getInstance(myProject)
        VIEW_STASH_ACTION = NotificationAction.createSimple(
                mySaver.saveMethod.selectBundleMessage(
                        GitBundle.message("rebase.notification.action.view.stash.text"),
                        GitBundle.message("rebase.notification.action.view.shelf.text")
                )
        ) { mySaver.showSavedChanges() }
    }

    fun rebase() {
        if (checkForRebasingPublishedCommits()) {
            GitFreezingProcess(myProject, GitBundle.message("rebase.git.operation.name")) { doRebase() }.execute()
        }
    }

    /**
     * Given a GitRebaseSpec this method either starts, or continues the ongoing rebase in multiple repositories.
     *
     *  * It does nothing with "already successfully rebased repositories" (the ones which have [GitRebaseStatus] == SUCCESSFUL,
     * and just remembers them to use in the resulting notification.
     *  * If there is a repository with rebase in progress, it calls `git rebase --continue` (or `--skip`).
     * It is assumed that there is only one such repository.
     *  * For all remaining repositories rebase on which didn't start yet, it calls `git rebase <original parameters>`
     *
     */
    private fun doRebase() {
        GitRebaseProcess.Companion.LOG.info("Started rebase")
        GitRebaseProcess.Companion.LOG.debug("Started rebase with the following spec: $myRebaseSpec")
        val statuses: MutableMap<GitRepository?, GitRebaseStatus> = LinkedHashMap(myRebaseSpec.statuses)
        val repositoriesToRebase = myRepositoryManager.sortByDependency(myRebaseSpec.incompleteRepositories)
        if (repositoriesToRebase.isEmpty()) {
            GitRebaseProcess.Companion.LOG.info("Nothing to rebase")
            return
        }
        try {
            DvcsUtil.workingTreeChangeStarted(myProject, GitBundle.message("activity.name.rebase"), Rebase).use { ignore ->
                if (!saveDirtyRootsInitially(repositoriesToRebase)) return
                var latestRepository: GitRepository? = null
                val showingIndicator = myProgressManager.progressIndicator
                val indicator = StepsProgressIndicator(
                        showingIndicator ?: EmptyProgressIndicator(),
                        repositoriesToRebase.size
                )
                indicator.isIndeterminate = false
                for (repository in repositoriesToRebase) {
                    var customMode: GitRebaseResumeMode? = null
                    if (repository === myRebaseSpec.ongoingRebase) {
                        customMode = myCustomMode ?: GitRebaseResumeMode.CONTINUE
                    }
                    val startHash = GitUtil.getHead(repository)
                    val rebaseStatus = rebaseSingleRoot(repository, customMode, GitRebaseProcess.Companion.getSuccessfulRepositories(statuses), indicator)
                    indicator.nextStep()
                    repository.update() // make the repo state info actual ASAP
                    if (customMode == GitRebaseResumeMode.CONTINUE) {
                        myDirtyScopeManager.dirDirtyRecursively(repository.root)
                    }
                    latestRepository = repository
                    statuses[repository] = rebaseStatus
                    GitUtil.refreshChangedVfs(repository, startHash)
                    if (rebaseStatus.type != GitRebaseStatus.Type.SUCCESS) {
                        break
                    }
                }
                val latestStatus = statuses[latestRepository]!!.getType()
                if (latestStatus == git4idea.rebase.GitRebaseStatus.Type.SUCCESS || latestStatus == git4idea.rebase.GitRebaseStatus.Type.NOT_STARTED) {
                    GitRebaseProcess.Companion.LOG.debug("Rebase completed successfully.")
                    mySaver.load()
                }
                if (latestStatus == git4idea.rebase.GitRebaseStatus.Type.SUCCESS) {
                    notifySuccess()
                }
                saveUpdatedSpec(statuses)
            }
        } catch (pce: ProcessCanceledException) {
            throw pce
        } catch (e: Throwable) {
            myRepositoryManager.ongoingRebaseSpec = null
            ExceptionUtil.rethrowUnchecked(e)
        }
    }

    private fun saveUpdatedSpec(statuses: Map<GitRepository?, git4idea.rebase.GitRebaseStatus>) {
        if (myRebaseSpec.shouldBeSaved()) {
            val newRebaseInfo = myRebaseSpec.cloneWithNewStatuses(statuses)
            myRepositoryManager.ongoingRebaseSpec = newRebaseInfo
        } else {
            myRepositoryManager.ongoingRebaseSpec = null
        }
    }

    private fun rebaseSingleRoot(repository: GitRepository,
                                 customMode: GitRebaseResumeMode?,
                                 alreadyRebased: Map<GitRepository, GitSuccessfulRebase>,
                                 indicator: ProgressIndicator): git4idea.rebase.GitRebaseStatus {
        var customMode = customMode
        val root = repository.root
        val repoName = DvcsUtil.getShortRepositoryName(repository)
        GitRebaseProcess.Companion.LOG.info("Rebasing root " + repoName + ", mode: " + ObjectUtils.notNull(customMode, "standard"))
        var retryWhenDirty = false
        var commitsToRebase = 0
        try {
            val params = myRebaseSpec.params
            if (params != null) {
                val upstream = params.upstream
                val branch = params.branch
                commitsToRebase = GitRebaseUtils.getNumberOfCommitsToRebase(repository, upstream, branch)
            }
        } catch (e: VcsException) {
            GitRebaseProcess.Companion.LOG.warn("Couldn't get the number of commits to rebase", e)
        }
        val progressListener = GitRebaseProcess.GitRebaseProgressListener(commitsToRebase, indicator)
        while (true) {
            val rebaseDetector = GitRebaseProblemDetector()
            val untrackedDetector = GitUntrackedFilesOverwrittenByOperationDetector(root)
            val rebaseCommandResult = callRebase(repository, customMode, rebaseDetector, untrackedDetector, progressListener)
            val result = rebaseCommandResult.commandResult
            val somethingRebased = customMode != null || progressListener.currentCommit > 1
            if (rebaseCommandResult.wasCancelledInCommitList()) {
                return git4idea.rebase.GitRebaseStatus.notStarted()
            } else if (rebaseCommandResult.wasCancelledInCommitMessage()) {
                showStoppedForEditingMessage()
                return GitRebaseStatus(git4idea.rebase.GitRebaseStatus.Type.SUSPENDED)
            } else if (result.success()) {
                if (rebaseDetector.hasStoppedForEditing()) {
                    showStoppedForEditingMessage()
                    return GitRebaseStatus(git4idea.rebase.GitRebaseStatus.Type.SUSPENDED)
                }
                GitRebaseProcess.Companion.LOG.debug("Successfully rebased $repoName")
                return GitSuccessfulRebase()
            } else if (rebaseDetector.isDirtyTree && customMode == null && !retryWhenDirty) {
                // if the initial dirty tree check doesn't find all local changes, we are still ready to stash-on-demand,
                // but only once per repository (if the error happens again, that means that the previous stash attempt failed for some reason),
                // and not in the case of --continue (where all local changes are expected to be committed) or --skip.
                GitRebaseProcess.Companion.LOG.debug("Dirty tree detected in $repoName")
                val saveError = saveLocalChanges(setOf(repository.root))
                retryWhenDirty = if (saveError == null) {
                    true // try same repository again
                } else {
                    GitRebaseProcess.Companion.LOG.warn(String.format(
                            "Couldn't %s root %s: %s",
                            if (mySaver.saveMethod === GitSaveChangesPolicy.SHELVE) "shelve" else "stash",
                            repository.root,
                            saveError
                    ))
                    showFatalError(saveError, repository, somethingRebased, alreadyRebased.keys)
                    val type = if (somethingRebased) git4idea.rebase.GitRebaseStatus.Type.SUSPENDED else git4idea.rebase.GitRebaseStatus.Type.ERROR
                    return GitRebaseStatus(type)
                }
            } else if (untrackedDetector.wasMessageDetected()) {
                GitRebaseProcess.Companion.LOG.info("Untracked files detected in $repoName")
                showUntrackedFilesError(untrackedDetector.relativeFilePaths, repository, somethingRebased, alreadyRebased.keys)
                val type = if (somethingRebased) git4idea.rebase.GitRebaseStatus.Type.SUSPENDED else git4idea.rebase.GitRebaseStatus.Type.ERROR
                return GitRebaseStatus(type)
            } else if (rebaseDetector.isNoChangeError) {
                GitRebaseProcess.Companion.LOG.info("'No changes' situation detected in $repoName")
                customMode = GitRebaseResumeMode.SKIP
            } else if (rebaseDetector.isMergeConflict) {
                GitRebaseProcess.Companion.LOG.info("Merge conflict in $repoName")
                val resolveResult = showConflictResolver(repository, false)
                customMode = if (resolveResult == GitRebaseProcess.ResolveConflictResult.ALL_RESOLVED) {
                    GitRebaseResumeMode.CONTINUE
                } else if (resolveResult == GitRebaseProcess.ResolveConflictResult.NOTHING_TO_MERGE) {
                    // the output is the same for the cases:
                    // (1) "unresolved conflicts"
                    // (2) "manual editing of a file not followed by `git add`
                    // => we check if there are any unresolved conflicts, and if not, then it is the case #2 which we are not handling
                    GitRebaseProcess.Companion.LOG.info("Unmerged changes while rebasing root " + repoName + ": " + result.errorOutputAsJoinedString)
                    showFatalError(result.errorOutputAsHtmlString, repository, somethingRebased, alreadyRebased.keys)
                    val type = if (somethingRebased) git4idea.rebase.GitRebaseStatus.Type.SUSPENDED else git4idea.rebase.GitRebaseStatus.Type.ERROR
                    return GitRebaseStatus(type)
                } else {
                    notifyNotAllConflictsResolved(repository)
                    return GitRebaseStatus(git4idea.rebase.GitRebaseStatus.Type.SUSPENDED)
                }
            } else {
                GitRebaseProcess.Companion.LOG.info("Error rebasing root " + repoName + ": " + result.errorOutputAsJoinedString)
                showFatalError(result.errorOutputAsHtmlString, repository, somethingRebased, alreadyRebased.keys)
                val type = if (somethingRebased) git4idea.rebase.GitRebaseStatus.Type.SUSPENDED else git4idea.rebase.GitRebaseStatus.Type.ERROR
                return GitRebaseStatus(type)
            }
        }
    }

    private fun callRebase(repository: GitRepository,
                           mode: GitRebaseResumeMode?,
                           vararg listeners: GitLineHandlerListener): GitRebaseCommandResult {
        return if (mode == null) {
            val params = Objects.requireNonNull(myRebaseSpec.params)
            myGit.rebase(repository, params!!, *listeners)
        } else if (mode == GitRebaseResumeMode.SKIP) {
            myGit.rebaseSkip(repository, *listeners)
        } else {
            GitRebaseProcess.Companion.LOG.assertTrue(mode == GitRebaseResumeMode.CONTINUE, "Unexpected rebase mode: $mode")
            myGit.rebaseContinue(repository, *listeners)
        }
    }

    @VisibleForTesting
    protected fun getDirtyRoots(repositories: Collection<GitRepository>): Collection<GitRepository> {
        return findRootsWithLocalChanges(repositories)
    }

    private fun saveDirtyRootsInitially(repositories: List<GitRepository>): Boolean {
        val repositoriesToSave: Collection<GitRepository> = ContainerUtil.filter(repositories) { repository: GitRepository ->
            repository != myRebaseSpec.ongoingRebase // no need to save anything when --continue/--skip is to be called
        }
        if (repositoriesToSave.isEmpty()) return true
        val rootsToSave = GitUtil.getRootsFromRepositories(getDirtyRoots(repositoriesToSave))
        val error = saveLocalChanges(rootsToSave)
        if (error != null) {
            myNotifier.notifyError(GitNotificationIdsHolder.REBASE_NOT_STARTED, GitBundle.message("rebase.notification.not.started.title"), error)
            return false
        }
        return true
    }

    @Nls
    private fun saveLocalChanges(rootsToSave: Collection<VirtualFile>): String? {
        return try {
            mySaver.saveLocalChanges(rootsToSave)
            null
        } catch (e: VcsException) {
            GitRebaseProcess.Companion.LOG.warn(e)
            val message = mySaver.saveMethod.selectBundleMessage(
                    GitBundle.message("rebase.notification.failed.stash.text"),
                    GitBundle.message("rebase.notification.failed.shelf.text")
            )
            HtmlBuilder().append(message).br().appendRaw(e.message).toString()
        }
    }

    private fun findRootsWithLocalChanges(repositories: Collection<GitRepository>): Collection<GitRepository> {
        return ContainerUtil.filter(repositories) { repository: GitRepository -> myChangeListManager.haveChangesUnder(repository.root) != ThreeState.NO }
    }

    @RequiresBackgroundThread
    protected open fun notifySuccess() {
        val rebasedBranch: String = GitRebaseProcess.Companion.getCommonCurrentBranchNameIfAllTheSame(myRebaseSpec.allRepositories)
        val params = myRebaseSpec.params
        var baseBranch = if (params == null) null else if (params.upstream != null) ObjectUtils.notNull(params.newBase, params.upstream!!) else params.newBase
        if (GitUtil.HEAD == baseBranch) {
            baseBranch = GitRebaseProcess.Companion.getItemIfAllTheSame<String>(myRebaseSpec.initialBranchNames.values, baseBranch)
        }
        val message = GitSuccessfulRebase.formatMessage(rebasedBranch, baseBranch, params != null && params.branch != null)
        myNotifier.notifyMinorInfo(GitNotificationIdsHolder.REBASE_SUCCESSFUL, GitBundle.message("rebase.notification.successful.title"), message)
    }

    private fun notifyNotAllConflictsResolved(conflictingRepository: GitRepository) {
        val description = GitRebaseUtils.mentionLocalChangesRemainingInStash(mySaver)
        val notification = VcsNotifier.IMPORTANT_ERROR_NOTIFICATION
                .createNotification(GitBundle.message("rebase.notification.conflict.title"), description, NotificationType.WARNING)
                .setDisplayId(GitNotificationIdsHolder.REBASE_STOPPED_ON_CONFLICTS)
                .addAction(createResolveNotificationAction(conflictingRepository))
                .addAction(CONTINUE_ACTION)
                .addAction(ABORT_ACTION)
        if (mySaver.wereChangesSaved()) notification.addAction(VIEW_STASH_ACTION)
        myNotifier.notify(notification)
    }

    private fun showConflictResolver(conflicting: GitRepository, calledFromNotification: Boolean): GitRebaseProcess.ResolveConflictResult {
        val params = GitConflictResolver.Params(myProject)
                .setMergeDialogCustomizer(createRebaseDialogCustomizer(conflicting, myRebaseSpec))
                .setReverse(true)
        val conflictResolver: GitRebaseProcess.RebaseConflictResolver = GitRebaseProcess.RebaseConflictResolver(myProject, conflicting, params, calledFromNotification)
        val allResolved = conflictResolver.merge()
        if (conflictResolver.myWasNothingToMerge) return GitRebaseProcess.ResolveConflictResult.NOTHING_TO_MERGE
        return if (allResolved) GitRebaseProcess.ResolveConflictResult.ALL_RESOLVED else GitRebaseProcess.ResolveConflictResult.UNRESOLVED_REMAIN
    }

    private fun showStoppedForEditingMessage() {
        val notification = VcsNotifier.IMPORTANT_ERROR_NOTIFICATION
                .createNotification(GitBundle.message("rebase.notification.editing.title"), "", NotificationType.INFORMATION)
                .setDisplayId(GitNotificationIdsHolder.REBASE_STOPPED_ON_EDITING)
                .addAction(CONTINUE_ACTION)
                .addAction(ABORT_ACTION)
        myNotifier.notify(notification)
    }

    private fun showFatalError(@Nls error: String,
                               currentRepository: GitRepository,
                               somethingWasRebased: Boolean,
                               successful: Collection<GitRepository>) {
        val descriptionBuilder = HtmlBuilder()
        if (myRepositoryManager.moreThanOneRoot()) {
            descriptionBuilder.append(DvcsUtil.getShortRepositoryName(currentRepository) + ": ")
        }
        descriptionBuilder.appendRaw(error).br()
        descriptionBuilder.appendRaw(GitRebaseUtils.mentionLocalChangesRemainingInStash(mySaver))
        val title = if (myRebaseSpec.ongoingRebase == null) GitBundle.message("rebase.notification.failed.rebase.title") else GitBundle.message("rebase.notification.failed.continue.title")
        val notification = VcsNotifier.IMPORTANT_ERROR_NOTIFICATION
                .createNotification(title, descriptionBuilder.toString(), NotificationType.ERROR)
                .setDisplayId(GitNotificationIdsHolder.REBASE_FAILED)
                .addAction(RETRY_ACTION)
        if (somethingWasRebased || !successful.isEmpty()) {
            notification.addAction(ABORT_ACTION)
        }
        if (mySaver.wereChangesSaved()) {
            notification.addAction(VIEW_STASH_ACTION)
        }
        myNotifier.notify(notification)
    }

    private fun showUntrackedFilesError(untrackedPaths: Set<String>,
                                        currentRepository: GitRepository,
                                        somethingWasRebased: Boolean,
                                        successful: Collection<GitRepository>) {
        val message = GitRebaseUtils.mentionLocalChangesRemainingInStash(mySaver)
        val actions: MutableList<NotificationAction> = ArrayList()
        actions.add(RETRY_ACTION)
        if (somethingWasRebased || !successful.isEmpty()) {
            actions.add(ABORT_ACTION)
        }
        if (mySaver.wereChangesSaved()) {
            actions.add(VIEW_STASH_ACTION)
        }
        GitUntrackedFilesHelper.notifyUntrackedFilesOverwrittenBy(
                myProject,
                currentRepository.root,
                untrackedPaths,
                GitBundle.message("rebase.git.operation.name"),
                message,
                *actions.toTypedArray<NotificationAction>()
        )
    }

    private fun checkForRebasingPublishedCommits(): Boolean {
        if (myCustomMode != null || myRebaseSpec.ongoingRebase != null) {
            return true
        }
        if (myRebaseSpec.params == null) {
            GitRebaseProcess.Companion.LOG.error("Shouldn't happen. Spec: $myRebaseSpec")
            return true
        }
        val upstream = myRebaseSpec.params!!.upstream
        for (repository in myRebaseSpec.allRepositories) {
            if (repository.currentBranchName == null) {
                GitRebaseProcess.Companion.LOG.error("No current branch in $repository")
                return true
            }
            val rebasingBranch = ObjectUtils.notNull(myRebaseSpec.params!!.branch, repository.currentBranchName!!)
            if (GitRebaseProcess.Companion.isRebasingPublishedCommit(repository, upstream, rebasingBranch)) {
                return GitRebaseProcess.Companion.askIfShouldRebasePublishedCommit()
            }
        }
        return true
    }

    private inner class RebaseConflictResolver internal constructor(project: Project,
                                                                    repository: GitRepository,
                                                                    params: Params, private val myCalledFromNotification: Boolean) : GitConflictResolver(project, setOf(repository.root), params) {
        private var myWasNothingToMerge = false
        override fun notifyUnresolvedRemain() {
            // will be handled in the common notification
        }

        @RequiresBackgroundThread
        override fun proceedAfterAllMerged(): Boolean {
            if (myCalledFromNotification) {
                retry(GitBundle.message("rebase.progress.indicator.continue.title"))
            }
            return true
        }

        override fun proceedIfNothingToMerge(): Boolean {
            myWasNothingToMerge = true
            return true
        }
    }

    private enum class ResolveConflictResult {
        ALL_RESOLVED,
        NOTHING_TO_MERGE,
        UNRESOLVED_REMAIN
    }

    private fun createResolveNotificationAction(currentRepository: GitRepository): NotificationAction {
        return NotificationAction.create(GitBundle.message("action.NotificationAction.text.resolve"),
                GitActionIdsHolder.Id.RESOLVE.id) { e: AnActionEvent?, notification: Notification ->
            myProgressManager.run(
                    object : Task.Backgroundable(myProject, GitBundle.message("rebase.progress.indicator.conflicts.collecting.title")) {
                        override fun run(indicator: ProgressIndicator) {
                            resolveConflicts(currentRepository, notification)
                        }
                    })
        }
    }

    private fun resolveConflicts(currentRepository: GitRepository, notification: Notification) {
        val result = showConflictResolver(currentRepository, true)
        if (result == GitRebaseProcess.ResolveConflictResult.NOTHING_TO_MERGE) {
            ApplicationManager.getApplication().invokeLater {
                val continueRebase = MessageDialogBuilder.yesNo(GitBundle.message("rebase.notification.all.conflicts.resolved.title"),
                        GitBundle.message("rebase.notification.all.conflicts.resolved.text"))
                        .yesText(GitBundle.message("rebase.notification.all.conflicts.resolved.continue.rebase.action.text"))
                        .noText(CommonBundle.getCancelButtonText())
                        .ask(myProject)
                if (continueRebase) {
                    retry(GitBundle.message("rebase.progress.indicator.continue.title"))
                    notification.expire()
                }
            }
        }
    }

    private fun abort() {
        myProgressManager.run(object : Task.Backgroundable(myProject, GitBundle.message("rebase.progress.indicator.aborting.title")) {
            override fun run(indicator: ProgressIndicator) {
                GitRebaseUtils.abort(myProject!!, indicator)
            }
        })
    }

    private fun retry(@Nls processTitle: String) {
        myProgressManager.run(object : Task.Backgroundable(myProject, processTitle, true) {
            override fun run(indicator: ProgressIndicator) {
                GitRebaseUtils.continueRebase(myProject!!)
            }
        })
    }

    private class GitRebaseProgressListener internal constructor(private val myCommitsToRebase: Int, private val myIndicator: ProgressIndicator) : GitLineHandlerListener {
        private var currentCommit = 0
        override fun onLineAvailable(line: String, outputType: Key<*>) {
            val matcher: Matcher = GitRebaseProcess.GitRebaseProgressListener.Companion.REBASING_PATTERN.matcher(line)
            if (matcher.matches()) {
                currentCommit = matcher.group(1).toInt()
            } else if (StringUtil.startsWith(line, GitRebaseProcess.GitRebaseProgressListener.Companion.APPLYING_PREFIX)) {
                currentCommit++
            }
            if (myCommitsToRebase != 0) {
                myIndicator.fraction = currentCommit.toDouble() / myCommitsToRebase
            }
        }

        companion object {
            @NonNls
            private val REBASING_PATTERN = Pattern.compile("^Rebasing \\((\\d+)/(\\d+)\\)$")

            @NonNls
            private val APPLYING_PREFIX = "Applying: "
        }
    }

    companion object {
        private val LOG = Logger.getInstance(GitRebaseProcess::class.java)
        private fun getCommonCurrentBranchNameIfAllTheSame(repositories: Collection<GitRepository>): @UnknownNullability String? {
            return ContainerUtil.map(repositories) { obj: GitRepository -> obj.currentBranchName }?.let { GitRebaseProcess.Companion.getItemIfAllTheSame<String>(it, null) }
        }

        @Contract("_, !null -> !null")
        private fun <T> getItemIfAllTheSame(collection: Collection<T>, defaultItem: T?): @UnknownNullability T? {
            return if (HashSet(collection).size == 1) ContainerUtil.getFirstItem(collection) else defaultItem
        }

        private fun getSuccessfulRepositories(statuses: Map<GitRepository, GitRebaseStatus>): Map<GitRepository, GitSuccessfulRebase> {
            val map: MutableMap<GitRepository, GitSuccessfulRebase> = LinkedHashMap()
            for (repository in statuses.keys) {
                val status = statuses[repository]
                if (status is GitSuccessfulRebase) map[repository] = status
            }
            return map
        }

        fun isRebasingPublishedCommit(repository: GitRepository,
                                      baseBranch: String?,
                                      rebasingBranch: String): Boolean {
            return try {
                val range = GitRebaseUtils.getCommitsRangeToRebase(baseBranch, rebasingBranch)
                val commits = GitHistoryUtils.collectTimedCommits(repository.project, repository.root, range)
                ContainerUtil.exists(commits) { commit: TimedVcsCommit? -> isCommitPublished(repository, commit!!.id) }
            } catch (e: VcsException) {
                GitRebaseProcess.Companion.LOG.error("Couldn't collect commits", e)
                true
            }
        }

        fun askIfShouldRebasePublishedCommit(): Boolean {
            val rebaseAnyway = Ref.create(false)
            val message = HtmlBuilder()
                    .append(GitBundle.message("rebase.confirmation.dialog.published.commits.message.first")).br()
                    .append(GitBundle.message("rebase.confirmation.dialog.published.commits.message.second"))
                    .wrapWith(HtmlChunk.html())
                    .toString()
            ApplicationManager.getApplication().invokeAndWait {
                val answer = DialogManager.showMessage(
                        message,
                        GitBundle.message("rebase.confirmation.dialog.published.commits.title"), arrayOf(
                        GitBundle.message("rebase.confirmation.dialog.published.commits.button.rebase.text"),
                        GitBundle.message("rebase.confirmation.dialog.published.commits.button.cancel.text")
                ),
                        1,
                        1,
                        Messages.getWarningIcon(),
                        null
                )
                rebaseAnyway.set(answer == 0)
            }
            return rebaseAnyway.get()
        }
    }
}

