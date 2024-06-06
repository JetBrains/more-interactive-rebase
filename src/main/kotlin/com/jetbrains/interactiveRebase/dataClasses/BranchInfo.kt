package com.jetbrains.interactiveRebase.dataClasses

import com.intellij.openapi.Disposable
import com.jetbrains.interactiveRebase.dataClasses.commands.FixupCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand

class BranchInfo(
    var name: String = "",
    var initialCommits: List<CommitInfo> = listOf(),
    var selectedCommits: MutableList<CommitInfo> = mutableListOf(),
    val isCheckedOut: Boolean = false,
) {
    private val listeners: MutableList<Listener> = mutableListOf()
    internal var currentCommits: MutableList<CommitInfo> = initialCommits.toMutableList()

    internal fun addListener(listener: Listener) = listeners.add(listener)

    /**
     * Sets name of
     * branch and notifies
     * listeners
     */
    internal fun setName(name: String) {
        this.name = name
        listeners.forEach { it.onNameChange(name) }
    }

    /**
     * Sets commits of
     * branch and notifies
     * listeners
     */
    internal fun setCommits(commits: List<CommitInfo>) {
        this.initialCommits = commits
        this.currentCommits = commits.toMutableList()
        listeners.forEach { it.onCommitChange(commits) }
    }

    /**
     * Adds to selected commits
     * of branch and notifies
     * listeners
     */
    internal fun addSelectedCommits(commit: CommitInfo) {
        selectedCommits.add(commit)
        listeners.forEach { it.onSelectedCommitChange(selectedCommits) }
    }

    /**
     * Removes from selected
     * commits of branch and
     * notifies listeners
     */
    internal fun removeSelectedCommits(commit: CommitInfo) {
        selectedCommits.remove(commit)
        commit.changes.forEach {
                change ->
            if (change is SquashCommand) {
                change.squashedCommits.forEach { selectedCommits.remove(it) }
            } else if (change is FixupCommand) {
                change.fixupCommits.forEach { selectedCommits.remove(it) }
            }
        }
        listeners.forEach { it.onSelectedCommitChange(selectedCommits) }
    }

    /**
     * Clears selected commits
     * of branch and notifies
     * listeners
     */
    internal fun clearSelectedCommits() {
        selectedCommits.forEach { it.isSelected = false }
        selectedCommits.clear()
        listeners.forEach { it.onSelectedCommitChange(selectedCommits) }
    }

    /**
     * Returns number of selected
     * commits excluding squashed commits
     */
    fun getActualSelectedCommitsSize(): Int {
        var size = 0
        selectedCommits.forEach { if (!it.isSquashed) size++ }

        return size
    }

    /**
     * Updates the order of the current
     * commits
     */
    internal fun updateCurrentCommits(
        oldIndex: Int,
        newIndex: Int,
        commit: CommitInfo,
    ) {
        currentCommits.removeAt(oldIndex)
        currentCommits.add(newIndex, commit)
        listeners.forEach { it.onCurrentCommitsChange(currentCommits) }
    }

    override fun toString(): String {
        return "BranchInfo(name='$name', initialCommits=$initialCommits, selectedCommits=$selectedCommits)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BranchInfo

        if (name != other.name) return false
        if (initialCommits != other.initialCommits) return false
        if (selectedCommits != other.selectedCommits) return false
        if (isCheckedOut != other.isCheckedOut) return false
        if (currentCommits != other.currentCommits) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + initialCommits.hashCode()
        result = 31 * result + selectedCommits.hashCode()
        result = 31 * result + isCheckedOut.hashCode()
        result = 31 * result + currentCommits.hashCode()
        return result
    }

    /**
     * Gets the index of the current commit
     * taking into account squashed commits
     */

    internal fun indexOfCommit(commit: CommitInfo): Int {
        var ret = currentCommits.indexOf(commit)

        if (commit.isSquashed) {
            commit.changes.forEach {
                if (it is FixupCommand) {
                    ret = currentCommits.indexOf(it.parentCommit)
                } else if (it is SquashCommand) {
                    ret = currentCommits.indexOf(it.parentCommit)
                }
            }
        }

        return ret
    }

    /**
     * Provides a listener
     * for changes in this class
     */
    interface Listener : Disposable {
        fun onNameChange(newName: String)

        fun onCommitChange(commits: List<CommitInfo>)

        fun onSelectedCommitChange(selectedCommits: MutableList<CommitInfo>)

        fun onCurrentCommitsChange(currentCommits: MutableList<CommitInfo>)

        override fun dispose() {}
    }
}
