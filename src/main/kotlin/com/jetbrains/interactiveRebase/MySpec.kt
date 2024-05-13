package com.jetbrains.interactiveRebase


import com.intellij.dvcs.DvcsUtil
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.util.containers.ContainerUtil
import git4idea.GitUtil
import git4idea.branch.GitRebaseParams
import git4idea.commands.Git
import git4idea.config.GitVcsSettings
import git4idea.i18n.GitBundle
import git4idea.repo.GitRepository
import git4idea.stash.GitChangesSaver
import org.jetbrains.annotations.NonNls
import java.util.*

class MySpec(val params: GitRebaseParams?,
                    private val myStatuses: Map<GitRepository, GitRebaseStatus>,
                    private val myInitialHeadPositions: Map<GitRepository, String>,
                    /**
                     * Returns names of branches which were current at the moment of this GitRebaseSpec creation. <br></br>
                     * The map may contain null elements, if some repositories were in the detached HEAD state.
                     */
                    val initialBranchNames: Map<GitRepository, String>,
                    val saver: GitChangesSaver,
                    private val myShouldBeSaved: Boolean) {

    val isValid: Boolean
        get() = singleOngoingRebase() && rebaseStatusesMatch()
    val allRepositories: Collection<GitRepository>
        get() = myStatuses.keys
    val ongoingRebase: GitRepository?
        get() = ContainerUtil.getFirstItem(ongoingRebases)
    val statuses: Map<GitRepository, GitRebaseStatus>
        get() = Collections.unmodifiableMap(myStatuses)
    val headPositionsToRollback: Map<GitRepository, String>
        get() = ContainerUtil.filter(myInitialHeadPositions) { repository: GitRepository -> myStatuses[repository]!!.type == GitRebaseStatus.Type.SUCCESS }

    fun cloneWithNewStatuses(statuses: Map<GitRepository, GitRebaseStatus>): MySpec {
        return MySpec(params, statuses, myInitialHeadPositions, initialBranchNames, saver, true)
    }

    fun shouldBeSaved(): Boolean {
        return myShouldBeSaved
    }

    val incompleteRepositories: List<GitRepository>
        /**
         * Returns repositories for which rebase is in progress, has failed and we want to retry, or didn't start yet. <br></br>
         * It is guaranteed that if there is a rebase in progress (returned by [.getOngoingRebase], it will be the first in the list.
         */
        get() {
            val incompleteRepositories: MutableList<GitRepository> = ArrayList()
            val ongoingRebase = ongoingRebase
            if (ongoingRebase != null) incompleteRepositories.add(ongoingRebase)
            incompleteRepositories.addAll(DvcsUtil.sortRepositories(ContainerUtil.filter(myStatuses.keys
            ) { repository: GitRepository -> repository != ongoingRebase && myStatuses[repository]!!.type != GitRebaseStatus.Type.SUCCESS }))
            return incompleteRepositories
        }
    private val ongoingRebases: Collection<GitRepository>
        private get() = ContainerUtil.filter(myStatuses.keys) { repository: GitRepository -> myStatuses[repository]!!.type == GitRebaseStatus.Type.SUSPENDED }

    private fun singleOngoingRebase(): Boolean {
        val ongoingRebases = ongoingRebases
        if (ongoingRebases.size > 1) {
            MySpec.Companion.LOG.warn("Invalid rebase spec: rebase is in progress in " + DvcsUtil.getShortNames(ongoingRebases))
            return false
        }
        return true
    }

    private fun rebaseStatusesMatch(): Boolean {
        for (repository in myStatuses.keys) {
            val savedStatus = myStatuses[repository]!!.type
            if (repository.isRebaseInProgress && savedStatus != GitRebaseStatus.Type.SUSPENDED) {
                MySpec.Companion.LOG.warn("Invalid rebase spec: rebase is in progress in " +
                        DvcsUtil.getShortRepositoryName(repository) + ", but it is saved as " + savedStatus)
                return false
            } else if (!repository.isRebaseInProgress && savedStatus == GitRebaseStatus.Type.SUSPENDED) {
                MySpec.Companion.LOG.warn("Invalid rebase spec: rebase is not in progress in " + DvcsUtil.getShortRepositoryName(repository))
                return false
            }
        }
        return true
    }

    @NonNls
    override fun toString(): String {
        val initialHeadPositions = StringUtil.join(myInitialHeadPositions.keys,
                { repository: GitRepository -> DvcsUtil.getShortRepositoryName(repository) + ": " + myInitialHeadPositions[repository] }, ", ")
        val statuses = StringUtil.join(myStatuses.keys,
                { repository: GitRepository -> DvcsUtil.getShortRepositoryName(repository) + ": " + myStatuses[repository] }, ", ")
        return String.format("{Params: [%s].\nInitial positions: %s.\nStatuses: %s.\nSaver: %s}", params, initialHeadPositions, statuses, saver)
    }

    companion object {
        private val LOG = Logger.getInstance(MySpec::class.java)
        fun forNewRebase(project: Project,
                         params: GitRebaseParams,
                         repositories: Collection<GitRepository>,
                         indicator: ProgressIndicator): MySpec {
            GitUtil.updateRepositories(repositories)
            val initialHeadPositions: Map<GitRepository, String> = MySpec.Companion.findInitialHeadPositions(repositories, params.branch)
            val initialBranchNames: Map<GitRepository, String> = MySpec.Companion.findInitialBranchNames(repositories)
            val initialStatusMap: MutableMap<GitRepository, GitRebaseStatus> = TreeMap(DvcsUtil.REPOSITORY_COMPARATOR)
            for (repository in repositories) {
                initialStatusMap[repository] = GitRebaseStatus.notStarted()
            }
            return MySpec(params, initialStatusMap, initialHeadPositions, initialBranchNames, MySpec.Companion.newSaver(project, indicator), true)
        }

        fun forResumeInSingleRepository(project: Project,
                                        repository: GitRepository,
                                        indicator: ProgressIndicator): MySpec? {
            if (!repository.isRebaseInProgress) return null
            val suspended = GitRebaseStatus(GitRebaseStatus.Type.SUSPENDED)
            return MySpec(null, Collections.singletonMap<GitRepository, GitRebaseStatus>(repository, suspended), emptyMap<GitRepository, String>(), emptyMap<GitRepository, String>(), MySpec.Companion.newSaver(project, indicator), false)
        }

        private fun newSaver(project: Project, indicator: ProgressIndicator): GitChangesSaver {
            val git = Git.getInstance()
            val saveMethod = GitVcsSettings.getInstance(project).saveChangesPolicy
            return GitChangesSaver.getSaver(project, git, indicator,
                    VcsBundle.message("stash.changes.message", GitBundle.message("rebase.operation.name")), saveMethod)
        }

        private fun findInitialHeadPositions(repositories: Collection<GitRepository>,
                                             branchToCheckout: String?): Map<GitRepository, String> {
            return ContainerUtil.map2Map(repositories) { repository: GitRepository ->
                val currentRevision: String? = MySpec.Companion.findCurrentRevision(repository, branchToCheckout)
                MySpec.Companion.LOG.debug("Current revision in [" + repository.root.name + "] is [" + currentRevision + "]")
                Pair.create(repository, currentRevision)
            }
        }

        private fun findCurrentRevision(repository: GitRepository, branchToCheckout: String?): String? {
            if (branchToCheckout != null) {
                val branch = repository.branches.findLocalBranch(branchToCheckout)
                if (branch != null) {
                    val hash = repository.branches.getHash(branch)
                    if (hash != null) {
                        return hash.asString()
                    } else {
                        MySpec.Companion.LOG.warn("The hash for branch [$branchToCheckout] is not known!")
                    }
                } else {
                    MySpec.Companion.LOG.warn("The branch [$branchToCheckout] is not known!")
                }
            }
            return repository.currentRevision
        }

        private fun findInitialBranchNames(repositories: Collection<GitRepository>): Map<GitRepository, String> {
            return ContainerUtil.map2Map(repositories) { repository: GitRepository ->
                val currentBranchName = repository.currentBranchName
                MySpec.Companion.LOG.debug("Current branch in [" + repository.root.name + "] is [" + currentBranchName + "]")
                Pair.create(repository, currentBranchName)
            }
        }
    }
}
