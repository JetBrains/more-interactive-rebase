package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xml.util.XmlStringUtil.wrapInHtml
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.GraphInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.FixupCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.ReorderCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import com.jetbrains.interactiveRebase.listeners.IRRepositoryChangeListener
import com.jetbrains.interactiveRebase.listeners.PopupListener
import com.jetbrains.interactiveRebase.utils.gitUtils.IRGitUtils
import com.jetbrains.rd.framework.base.deepClonePolymorphic
import git4idea.GitUtil
import git4idea.merge.GitConflictResolver
import git4idea.repo.GitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.AWTEvent
import java.awt.Toolkit

@Service(Service.Level.PROJECT)
class ModelService(
    private val project: Project,
    val coroutineScope: CoroutineScope,
    private val commitService: CommitService,
) : Disposable {
    constructor(project: Project, coroutineScope: CoroutineScope) : this(project, coroutineScope, project.service<CommitService>())

    var branchInfo = BranchInfo()
    var graphInfo = GraphInfo(branchInfo)
    var fetched = false
    private val graphService = project.service<GraphService>()
    private val dialogService = project.service<DialogService>()
    internal val invoker = project.service<RebaseInvoker>()
    internal val repositoryChangeListener = IRRepositoryChangeListener(project)
    private val repo = GitUtil.getRepositoryManager(project).getRepositoryForRootQuick(project.guessProjectDir())
    internal var rebaseInProcess: Boolean = false
    internal var previousConflictCommit: String = ""
    internal var gitDialog: GitConflictResolver? = null
    var gitUtils = IRGitUtils(project)
    internal var cherryPickInProcess: Boolean = false
    internal var previousCherryCommit: String = ""
    var isDoneCherryPicking = true
    var noMoreCherryPicking = false
    var counterForCherry = 0

    /**
     * Fetches current branch info
     * on creation and subscribes
     * to the GitRefreshListener
     */
    init {
        fetchGraphInfo(0)
        populateLocalBranches(0)
        project.messageBus.connect(this).subscribe(GitRepository.GIT_REPO_CHANGE, repositoryChangeListener)

        Toolkit.getDefaultToolkit().addAWTEventListener(PopupListener(project), AWTEvent.WINDOW_EVENT_MASK)
        if (repo != null) rebaseInProcess = repo.isRebaseInProgress
    }

    /**
     * Selects the single passed in
     * commit by also deselecting all
     * currently selected commits
     */
    fun selectSingleCommit(
        commit: CommitInfo,
        branchInfo: BranchInfo,
    ) {
        clearSelectedCommits()
        addToSelectedCommits(commit, branchInfo)
    }

    /**
     * Adds a commit to the list
     * of selected commits without
     * deselecting the rest of the
     * commits
     */
    fun addToSelectedCommits(
        commit: CommitInfo,
        branchInfo: BranchInfo,
    ) {
        if (commit.isCollapsed) return
        commit.isSelected = true
        branchInfo.addSelectedCommits(commit)
        commit.getChangesAfterPick().forEach { change ->
            if (change is FixupCommand || change is SquashCommand) {
                project.service<ActionService>().getCombinedCommits(change).forEach {
                    branchInfo.addSelectedCommits(it)
                }
            }
        }
    }

    /**
     * Removes commit from
     * list of selected commits
     * for a given branch
     */
    fun removeFromSelectedCommits(
        commit: CommitInfo,
        branchInfo: BranchInfo,
    ) {
        commit.isSelected = false
        if (commit.isCollapsed) return
        branchInfo.removeSelectedCommits(commit)
    }

    /**
     * Marks a commit as a reordered by
     * 1. sets the isReordered flag to true
     * 2. adds a ReorderCommand
     * to the visual changes applied to the commit
     * 3. adds the Reorder Command to the Invoker
     * that holds an overview of all staged changes.
     */
    internal fun markCommitAsReordered(
        commit: CommitInfo,
        oldIndex: Int,
        newIndex: Int,
    ) {
        commit.setReorderedTo(true)
        val command =
            ReorderCommand(
                commit,
                oldIndex,
                newIndex,
            )
        commit.addChange(command)
        project.service<RebaseInvoker>().addCommand(command)
    }

    /**
     * Returns the selected
     * commits
     */
    fun getSelectedCommits(): MutableList<CommitInfo> {
        return getSelectedBranch().selectedCommits
    }

    /**
     * Clears the selected commits
     * from both the main and the added
     * branch
     */
    fun clearSelectedCommits() {
        graphInfo.mainBranch.clearSelectedCommits()
        graphInfo.addedBranch?.clearSelectedCommits()
    }

    /**
     * Returns the selected commit which is the lowest visually in the list.
     */
    fun getLowestSelectedCommit(): CommitInfo {
        var commit = getSelectedCommits()[0]
        var index = getCurrentCommits().indexOf(commit)

        getSelectedCommits().forEach {
            if (getCurrentCommits().indexOf(it) > index && !it.isSquashed) {
                commit = it
                index = getCurrentCommits().indexOf(it)
            }
        }

        return commit
    }

    /**
     * Returns the selected commit which is the highest visually in the list.
     */
    fun getHighestSelectedCommit(): CommitInfo {
        val branch = getSelectedBranch()
        var commit = branch.selectedCommits[0]
        var index = branch.currentCommits.indexOf(commit)

        branch.selectedCommits.forEach {
            if (branch.currentCommits.indexOf(it) < index && !it.isSquashed) {
                commit = it
                index = branch.currentCommits.indexOf(it)
            }
        }

        return commit
    }

    /**
     * Returns the last commit that is selected
     * but is not squashed or fixed up
     */
    fun getLastSelectedCommit(): CommitInfo {
        var commit = getSelectedCommits().last()

        // Ensure that the commit we are moving is actually displayed
        while (commit.isSquashed) {
            val index = getSelectedCommits().indexOf(commit)
            commit = getSelectedCommits()[index - 1]
        }

        return commit
    }

    /**
     * Returns the current
     * displayed commits
     */
    fun getCurrentCommits(): MutableList<CommitInfo> {
        return getSelectedBranch().currentCommits
    }

    /**
     * Fetches and updates the graph
     * info inside a
     * coroutine
     */
    fun fetchGraphInfo(n: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                graphService.updateGraphInfo(graphInfo)
            } catch (e: VcsException) {
                if (n < 3) {
                    fetchGraphInfo(n + 1)
                } else {
                    showWarningGitDialogClosesPlugin("There was an error while fetching data from Git.", dialogService)
                }
            }

            coroutineScope.launch(Dispatchers.EDT) {
                project.service<ActionService>().mainPanel.graphPanel.updateGraphPanel()
            }
        }
    }

    /**
     * Populates the GraphInfo field in order to be able to display the side panel of local branches
     */
    fun populateLocalBranches(n: Int) {
        val branchService = project.service<BranchService>()
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val list = branchService.getBranchesExceptCheckedOut()
                if (!list.isNullOrEmpty()) {
                    graphInfo.branchList = list.toMutableList()
                }
            } catch (e: VcsException) {
                if (n < 3) {
                    populateLocalBranches(n + 1)
                }
            }
        }
    }

    /**
     * Populates the added branch field in the graph info with the given branch
     */
    fun addSecondBranchToGraphInfo(
        addedBranch: String,
        n: Int,
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                graphService.addBranch(graphInfo, addedBranch)
            } catch (e: VcsException) {
                if (n < 3) {
                    addSecondBranchToGraphInfo(addedBranch, n + 1)
                } else {
                    showWarningGitDialogForBranch("This branch cannot be added.")
                    project.service<ActionService>().mainPanel.sidePanel.sideBranchPanels
                        .find { it.isSelected }?.deselectBranch()
                    project.service<ActionService>().mainPanel.sidePanel.resetAllBranchesVisually()
                }
            }
        }
    }

    /**
     * Removes the added branch field in the graph info
     */
    fun removeSecondBranchFromGraphInfo(n: Int) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                graphService.removeBranch(graphInfo)
            } catch (e: VcsException) {
                if (n < 3) {
                    removeSecondBranchFromGraphInfo(n + 1)
                }
                // TODO Handle
            }
        }
    }

    internal fun markRebaseCommitAsPaused(head: String) {
        for (commit in branchInfo.currentCommits.reversed()) {
            if (commit.commit.id.toString() == head) {
                commit.markAsPaused()
                break
            } else {
                if (commit.isPaused) commit.markAsNotPaused()
                commit.markAsRebased()
            }
        }
    }

    internal fun removeAllChangesIfNeeded() {
        project.service<RebaseInvoker>().commands.clear()
        project.service<RebaseInvoker>().undoneCommands.clear()
        graphInfo.mainBranch.initialCommits.forEach {
                c ->
            c.isSquashed = false
            c.isRebased = false
            c.isPaused = false
            c.isCollapsed = false
            c.changes.clear()
        }
        graphInfo.addedBranch?.initialCommits?.forEach {
                c ->
            c.wasCherryPicked = false
            c.changes.clear()
        }
        graphInfo.mainBranch.currentCommits = graphInfo.mainBranch.initialCommits.toMutableList()
    }

    /**
     * Re-fetches everything and clears all saved fields in the model.
     */
    internal fun refreshModel() {
        if (rebaseInProcess) {
            removeAllChangesIfNeeded()
            rebaseInProcess = false
            previousConflictCommit = ""
            project.service<ActionService>().switchToChangeButtonsIfNeeded()
            project.service<ActionService>().mainPanel.graphPanel.updateGraphPanel()
        }
        fetchGraphInfo(0)
        populateLocalBranches(0)
    }

    /**
     * Refreshes the model during a rebase process, such that the current commit is marked as paused
     * and in the case of conflicts it re-triggers the popup with merge conflicts.
     */
    internal fun refreshModelDuringRebaseProcess(root: VirtualFile) {
        rebaseInProcess = true
        val currentCommit = gitUtils.getCurrentRebaseCommit(project, root)
        markRebaseCommitAsPaused(currentCommit)
        project.service<ActionService>().switchToRebaseProcessPanel()
    }

    /**
     * When there is a problem with fetching the git information and displaying the graph
     * the dialog pops up and when clicked closes the plugin
     */
    fun showWarningGitDialogClosesPlugin(
        description: String,
        dialogService: DialogService,
    ) {
        coroutineScope.launch(Dispatchers.EDT) {
            dialogService.warningOkCancelDialog(
                "Git Issue",
                description,
            )
            project.service<IRVirtualFileService>().closeIRVirtualFile()
        }
    }

    /**
     * When there is a problem with fetching the git information for the second branch
     * the dialog pops up and deselects the branch
     */
    fun showWarningGitDialogForBranch(description: String) {
        coroutineScope.launch(Dispatchers.EDT) {
            dialogService.warningOkCancelDialog(
                "Git Issue",
                description,
            )
        }
    }

    /**
     * Return the branchInfo of the branch
     * that has selected commits
     */
    fun getSelectedBranch(): BranchInfo {
        if (!areDisabledCommitsSelected()) {
            return graphInfo.mainBranch
        }

        return graphInfo.addedBranch!!
    }

    /**
     * Returns true if the there are currently any selected commits on the added branch,
     * false otherwise or if there is no added branch
     */
    internal fun areDisabledCommitsSelected(): Boolean {
        val added = graphInfo.addedBranch
        return (added != null && added.selectedCommits.isNotEmpty())
    }

    /**
     * Creates our own merge conflict dialog for the commit,
     * which has the current commit message and hash in it.
     */
    fun createMergeConflictDialogForCommit(
        currentCommitHash: String,
        root: VirtualFile,
    ) {
        val params = GitConflictResolver.Params(project)
        params.setReverse(true)
        params.setErrorNotificationTitle("Conflicts during rebasing.")
        params.setErrorNotificationAdditionalDescription("Please resolve the conflicts and press continue to proceed with the rebase")

        val commit = branchInfo.currentCommits.find { it.commit.id.toString() == currentCommitHash }!!.commit
        val mergeConflictDescription = wrapInHtml("Conflicts in commit <b>" + commit.subject + "</b> (" + commit.id.toShortString() + ")")
        params.setMergeDescription(mergeConflictDescription)

        coroutineScope.launch(Dispatchers.IO) {
            val dialog = gitDialog ?: GitConflictResolver(project, mutableListOf(root), params)
            dialog.mergeNoProceed()
        }
    }

    /**
     * This sets all previous commits as rebased and the current commit as paused,
     * keeps track of the current commit and shows the custom merge dialog.
     */
    fun showCustomMergeDialog(
        currentMergingCommit: String,
        root: VirtualFile,
    ) {
        previousConflictCommit = currentMergingCommit
        markRebaseCommitAsPaused(currentMergingCommit)
        createMergeConflictDialogForCommit(currentMergingCommit, root)
    }

    /**
     * Makes a deep copy of GraphInfo
     */
    fun duplicateGraphInfo(graphReference: GraphInfo): GraphInfo {
        return graphReference.copy(
            mainBranch = duplicateBranchInfo(graphReference.mainBranch),
            addedBranch = if (graphReference.addedBranch == null) null else duplicateBranchInfo(graphReference.addedBranch!!),
        )
    }

    /**
     * Makes a deep-copy of BranchInfo
     */
    fun duplicateBranchInfo(branchReference: BranchInfo): BranchInfo {
        val copy =
            branchReference.copy(
                initialCommits = branchReference.initialCommits.map { duplicateCommitInfo(it) },
            )
        copy.baseCommit = if (branchReference.baseCommit == null) null else duplicateCommitInfo(branchReference.baseCommit!!)
        copy.currentCommits = branchReference.currentCommits.map { duplicateCommitInfo(it) }.toMutableList()
        return copy
    }

    /**
     * Makes a deep-copy of CommitInfo for the given fields
     */
    fun duplicateCommitInfo(commitReference: CommitInfo): CommitInfo {
        return commitReference.copy(
            isSelected = false,
            isHovered = false,
            isDragged = false,
            isCollapsed = commitReference.isCollapsed,
            changes = commitReference.changes.deepClonePolymorphic(),
        )
    }

    /**
     * Dispose routine
     */
    override fun dispose() {
    }
}
