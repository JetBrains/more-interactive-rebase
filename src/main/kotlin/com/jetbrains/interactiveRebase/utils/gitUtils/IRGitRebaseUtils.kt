package com.jetbrains.interactiveRebase.utils.gitUtils

import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.util.Consumer
import com.jetbrains.interactiveRebase.dataClasses.commands.CherryCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.IRCommand
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.GraphService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import git4idea.GitCommit
import git4idea.GitUtil
import git4idea.branch.GitRebaseParams
import git4idea.cherrypick.GitCherryPicker
import git4idea.history.GitHistoryUtils
import git4idea.i18n.GitBundle
import git4idea.rebase.GitRebaseEditorHandler
import git4idea.rebase.GitRebaseUtils
import git4idea.repo.GitRepository
import git4ideaClasses.GitRebaseEntryGeneratedUsingLog
import git4ideaClasses.IREditorHandler
import git4ideaClasses.IRGitEntry
import git4ideaClasses.IRGitModel

/**
 * Class for connection with the rebasing functionality of JetBrains
 */
class IRGitRebaseUtils(private val project: Project) {
    private val repo = GitUtil.getRepositoryManager(project).getRepositoryForRootQuick(project.guessProjectDir())

    /**
     * Start Cherry-Picking
     */
    internal fun cherryPick(commands: List<IRCommand>) {
        object : Task.Backgroundable(project, GitBundle.message("rebase.progress.indicator.preparing.title")) {
            override fun run(indicator: ProgressIndicator) {
                val modelService = project.service<ModelService>()
                val cherryCommands = commands.filterIsInstance<CherryCommand>()
                var wasReordered = false
                cherryCommands.forEachIndexed { index, command ->
                    val base = command.baseCommit
                    val newbie = command.commit
                    if (command.index != 0) {
                        wasReordered = true
                    }
                    GitCherryPicker(project).cherryPick(mutableListOf(base.commit))
                    try {
                        var head: GitCommit? = null
                        val consumer =
                            Consumer<GitCommit> {
                                    commit ->
                                head = commit
                            }
                        GitHistoryUtils.loadDetails(project, repo?.root!!, consumer, "-n", "1")
                        var previousHead = modelService.branchInfo.initialCommits[0].commit
                        if (index != 0) {
                            previousHead = cherryCommands[index - 1].commit.commit
                        }
                        if (previousHead == head) {
                            var output = IRGitUtils(project).gitReset()
                            modelService.noMoreCherryPicking = true
                            project.service<ModelService>().removeAllChangesIfNeeded()
                            project.service<GraphService>().updateGraphInfo(project.service<ModelService>().graphInfo)
                            project.service<ActionService>().mainPanel.graphPanel.updateGraphPanel()
                            return
                        }
                        newbie.commit = head!!
                        project.service<RebaseInvoker>().commands.remove(command)
                    } catch (e: VcsException) {
                        println("Trying to display parents of initial commit")
                    }
                }
                modelService.noMoreCherryPicking = true
                project.service<ModelService>().graphInfo.mainBranch.currentCommits.forEach { c ->
                    c.wasCherryPicked = false
                    c.changes.removeAll { it is CherryCommand }
                }
                project.service<ModelService>().graphInfo.addedBranch?.initialCommits?.forEach {
                        c ->
                    c.wasCherryPicked = false
                }
                project.service<ActionService>().mainPanel.graphPanel.updateGraphPanel()
                if (project.service<RebaseInvoker>().commands.isNotEmpty() || wasReordered) {
                    project.service<RebaseInvoker>().executeCommands()
                } else {
                    project.service<ModelService>().removeAllChangesIfNeeded()
                    project.service<ActionService>().mainPanel.graphPanel.updateGraphPanel()
                    project.service<ModelService>().fetchGraphInfo(0)
                    project.service<ModelService>().populateLocalBranches(0)
                }
            }
        }.queue()
    }

    /**
     * Prepares for rebase. Runs the preparation in the background. Maybe it is not necessary.
     * The commit that is passed is the initial commit of the branch
     * and is necessary as an argument to call the rebase functionality
     * TODO(optimize and refactor)
     */
    internal fun rebase(
        commit: String,
        model: IRGitModel<GitRebaseEntryGeneratedUsingLog>,
    ) {
        object : Task.Backgroundable(project, GitBundle.message("rebase.progress.indicator.preparing.title")) {
            override fun run(indicator: ProgressIndicator) {
                startInteractiveRebase(commit, repo?.let { IRGitEditorHandler(it, model) })
            }
        }.queue()
    }

    /**
     * Calls the rebase functionality in the git4idea rebase. Runs the task in the background.
     */
    internal fun startInteractiveRebase(
        commit: String,
        editorHandler: GitRebaseEditorHandler? = null,
    ) {
        object : Task.Backgroundable(project, GitBundle.message("rebase.progress.indicator.title")) {
            override fun run(indicator: ProgressIndicator) {
                IdeFocusManager.getInstance(project).requestFocus(project.service<ActionService>().mainPanel, true)
                val params =
                    repo?.vcs?.let {
                        GitRebaseParams.editCommits(
                            it.version,
                            commit,
                            editorHandler,
                            false,
                        )
                    }
                if (params != null) {
                    GitRebaseUtils.rebase(project, listOf(repo), params, indicator)
                }
            }
        }.queue()
    }
}

/**
 * Handler for the interactive rebasing
 */
internal class IRGitEditorHandler(
    repository: GitRepository,
    private val rebaseTodoModel: IRGitModel<GitRebaseEntryGeneratedUsingLog>,
) :
    IREditorHandler(repository.project, repository.root) {
    private var rebaseFailed = false

    /**
     * Collects the new entries for the model
     */
    override fun collectNewEntries(entries: List<IRGitEntry>): List<IRGitEntry>? {
        if (rebaseFailed) {
            return super.collectNewEntries(entries)
        }
        processModel(rebaseTodoModel)
        return rebaseTodoModel.convertToEntries()
    }
}
