package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.FixupCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.RebaseCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.ReorderCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import com.jetbrains.interactiveRebase.utils.gitUtils.IRGitRebaseUtils
import git4ideaClasses.GitRebaseEntryGeneratedUsingLog
import git4ideaClasses.IRGitEntry
import git4ideaClasses.IRGitModel

@Service(Service.Level.PROJECT)
class RebaseInvoker(val project: Project) {
    var branchInfo = BranchInfo()
    internal lateinit var model: IRGitModel<GitRebaseEntryGeneratedUsingLog>

    /**
     * Creates a git model for the rebase, from the
     * correct list of current commits.
     */
    fun createModel() {
        expandCurrentCommits()
        val commits =
            branchInfo.currentCommits.map {
                    commitInfo ->
                GitRebaseEntryGeneratedUsingLog(commitInfo.commit)
            }

        model = convertToModel(commits.reversed())
    }

    /**
     * Method that expands the previously changed list of current commits.
     * At the moment, we remove the "squashed" and "fixed up" commits from the list,
     * but we add them back at the correct position.
     */
    fun expandCurrentCommits() {
        val commits = branchInfo.currentCommits.toMutableList()
        for (commitInfo in branchInfo.currentCommits) {
            for (command in commitInfo.changes) {
                if (command is SquashCommand) {
                    val parentCommit = command.parentCommit
                    val parentIndex = commits.indexOfFirst { it == parentCommit }
                    if (parentIndex != -1) {
                        commits.addAll(parentIndex, command.squashedCommits)
                    }
                }
                if (command is FixupCommand) {
                    val parentCommit = command.parentCommit
                    val parentIndex = commits.indexOfFirst { it == parentCommit }
                    if (parentIndex != -1) {
                        commits.addAll(parentIndex, command.fixupCommits)
                    }
                }
            }
        }
        branchInfo.currentCommits = commits
    }

    /**
     * Global (project-level) list of rebase commands
     * that will be executed, once the rebase is initiated.
     */
    var commands = mutableListOf<RebaseCommand>()

    /**
     * Converts the entries to a model
     */
    private fun <T : IRGitEntry> convertToModel(entries: List<T>): IRGitModel<T> {
        val result = mutableListOf<IRGitModel.Element<T>>()
        // consider auto-squash
        entries.forEach { entry ->
            val index = result.size
            when (entry.action) {
                IRGitEntry.Action.PICK, IRGitEntry.Action.REWORD -> {
                    val type = IRGitModel.Type.NonUnite.KeepCommit.Pick
                    result.add(IRGitModel.Element.Simple(index, type, entry))
                }
                IRGitEntry.Action.EDIT -> {
                    val type = IRGitModel.Type.NonUnite.KeepCommit.Edit
                    result.add(IRGitModel.Element.Simple(index, type, entry))
                }
                IRGitEntry.Action.DROP -> {
                    // move them to the end
                }
                IRGitEntry.Action.FIXUP, IRGitEntry.Action.SQUASH -> {
                    val lastElement = result.lastOrNull() ?: throw IllegalArgumentException("Couldn't unite with non-existed commit")
                    val root =
                        when (lastElement) {
                            is IRGitModel.Element.UniteChild<T> -> lastElement.root
                            is IRGitModel.Element.UniteRoot<T> -> lastElement
                            is IRGitModel.Element.Simple<T> -> {
                                when (val rootType = lastElement.type) {
                                    is IRGitModel.Type.NonUnite.KeepCommit -> {
                                        val newRoot = IRGitModel.Element.UniteRoot(lastElement.index, rootType, lastElement.entry)
                                        result[newRoot.index] = newRoot
                                        newRoot
                                    }
                                    is IRGitModel.Type.NonUnite.Drop, is IRGitModel.Type.NonUnite.UpdateRef -> {
                                        throw IllegalStateException()
                                    }
                                }
                            }
                        }
                    val element = IRGitModel.Element.UniteChild(index, entry, root)
                    root.addChild(element)
                    result.add(element)
                }
                IRGitEntry.Action.UPDATEREF -> {
                    val type = IRGitModel.Type.NonUnite.UpdateRef
                    val element = IRGitModel.Element.Simple(index, type, entry)
                    result.add(element)
                }
                is IRGitEntry.Action.Other -> throw IllegalArgumentException("Couldn't convert unknown action to the model")
            }
        }
        entries.filter { it.action is IRGitEntry.Action.DROP }.forEach { entry ->
            val index = result.size
            result.add(IRGitModel.Element.Simple(index, IRGitModel.Type.NonUnite.Drop, entry))
        }

        return IRGitModel(result)
    }

    /**
     * Adds a command to the list of commands to be executed.
     */
    fun addCommand(command: RebaseCommand) {
        commands.add(command)
    }

    /**
     * Removes a command from the list of commands to be executed.
     */
    fun removeCommand(command: RebaseCommand) {
        commands.remove(command)
    }

    /**
     * Executes all the commands to be able to perform the rebase.
     */
    fun executeCommands() {
        val commandz = commands.filterNot { it is ReorderCommand }
        commandz.forEach { it.execute(model, branchInfo) }
        IRGitRebaseUtils(project).rebase(branchInfo.initialCommits.reversed()[0].commit, model)
        commands.clear()
    }
}
