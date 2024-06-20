package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo

import com.jetbrains.interactiveRebase.dataClasses.commands.CherryCommand
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.CollapseCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.FixupCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.IRCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.RebaseCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.ReorderCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import com.jetbrains.interactiveRebase.utils.gitUtils.IRGitRebaseUtils
import git4idea.GitCommit
import git4ideaClasses.GitRebaseEntryGeneratedUsingLog
import git4ideaClasses.IRGitEntry
import git4ideaClasses.IRGitModel

@Service(Service.Level.PROJECT)
class RebaseInvoker(val project: Project) {
    var branchInfo = BranchInfo()

    internal lateinit var model: IRGitModel<GitRebaseEntryGeneratedUsingLog>

    /**
     * Global (project-level) list of rebase commands
     * that will be executed, once the rebase is initiated.
     */

    var commands = mutableListOf<IRCommand>()
    var undoneCommands = mutableListOf<IRCommand>()
    var commitsToDisplayDuringRebase = mutableListOf<CommitInfo>()

    fun removeChangesBeforePick() {
        val changesToRemove = mutableListOf<IRCommand>()
        for (commitInfo in branchInfo.currentCommits) {
            val changes = commitInfo.changes.filter { it !in commitInfo.getChangesAfterPick() }
            changesToRemove.addAll(changes)
        }
        commands.removeAll(changesToRemove)
    }

    /**
     * Creates a git model for the rebase, from the
     * correct list of current commits.
     */
    fun createModel() {
        expandCollapsedCommits()

        commitsToDisplayDuringRebase = branchInfo.currentCommits.toMutableList()
        expandCurrentCommitsForSquashed()
        removeChangesBeforePick()
        val commits =
            branchInfo.currentCommits.map {
                    commitInfo ->
                GitRebaseEntryGeneratedUsingLog(commitInfo.commit)
            }

        model = convertToModel(commits.reversed())
    }

    fun expandCollapsedCommits() {
        val commits = branchInfo.currentCommits.toMutableList()
        for (commitInfo in branchInfo.currentCommits) {
            var collapseComm: CollapseCommand? = null
            for (command in commitInfo.changes) {
                if (command is CollapseCommand) {
                    val parentCommit = command.firstCommit
                    val parentIndex = commits.indexOfFirst { it == parentCommit }
                    commits.addAll(parentIndex, command.collapsedCommits)
                    command.collapsedCommits.forEach { it.removeChange(command) }
                    collapseComm = command
                }
            }
            commitInfo.isCollapsed = false
            if (collapseComm != null) {
                commitInfo.removeChange(collapseComm)
            }
        }
        branchInfo.currentCommits = commits
    }

    /**
     * Method that expands the previously changed list of current commits.
     * At the moment, we remove the "squashed" and "fixed up" commits from the list,
     * but we add them back at the correct position.
     */
    fun expandCurrentCommitsForSquashed() {
        val commits = branchInfo.currentCommits.toMutableList()
        for (commitInfo in branchInfo.currentCommits) {
            for (command in commitInfo.getChangesAfterPick()) {
                if (command is SquashCommand) {
                    val parentCommit = command.parentCommit
                    val parentIndex = commits.indexOfFirst { it == parentCommit }
                    commits.addAll(parentIndex, command.squashedCommits)
                }
                if (command is FixupCommand) {
                    val parentCommit = command.parentCommit
                    val parentIndex = commits.indexOfFirst { it == parentCommit }
                    commits.addAll(parentIndex, command.fixupCommits)
                }
            }
        }
        branchInfo.currentCommits = commits
    }

    /**
     * Converts the entries to a model
     */
    internal fun <T : IRGitEntry> convertToModel(entries: List<T>): IRGitModel<T> {
        val result = mutableListOf<IRGitModel.Element<T>>()
        // consider auto-squash
        entries.forEach { entry ->
            val index = result.size
            val type = IRGitModel.Type.NonUnite.KeepCommit.Pick
            result.add(IRGitModel.Element.Simple(index, type, entry))
        }
        return IRGitModel(result)
    }

    /**
     * Adds a command to the list of commands to be executed.
     */
    fun addCommand(command: IRCommand) {
        commands.add(command)
    }

    /**
     * Removes a command from the list of commands to be executed.
     */
    fun removeCommand(command: IRCommand) {
        commands.remove(command)
    }

    fun executeCherry(){
        expandCollapsedCommits()
        IRGitRebaseUtils(project).cherryPick(commands)
    }

    /**
     * Executes all the commands to be able to perform the rebase.
     */
    fun executeCommands() {
        createModel()
        val commandz = commands.filterNot { it is ReorderCommand }
        var base = branchInfo.initialCommits.reversed()[0].commit.parents.first().asString()
        val rebaseCommand = commandz.findLast { command -> command is RebaseCommand }
        if (rebaseCommand != null) {
            base = (rebaseCommand as RebaseCommand).commit.commit.id.asString()
        }
        commandz.forEach {
            it.execute(model, branchInfo)
        }
        if (base != null) {
            IRGitRebaseUtils(project).rebase(base, model)
            branchInfo.currentCommits = commitsToDisplayDuringRebase
            commitsToDisplayDuringRebase = mutableListOf()
        }
        commands.clear()
    }
}
