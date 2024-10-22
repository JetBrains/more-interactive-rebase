package com.jetbrains.interactiveRebase.dataClasses

import com.intellij.openapi.Disposable
import com.jetbrains.interactiveRebase.dataClasses.commands.CollapseCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.FixupCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import com.jetbrains.rd.framework.base.deepClonePolymorphic

data class BranchInfo(
    var name: String = "",
    var initialCommits: List<CommitInfo> = listOf(),
    var selectedCommits: MutableList<CommitInfo> = mutableListOf(),
    var isPrimary: Boolean = false,
    var isWritable: Boolean = true,
    var isRebased: Boolean = false,
) {
    private val listeners: MutableList<Listener> = mutableListOf()
    var currentCommits: MutableList<CommitInfo> = initialCommits.toMutableList()
    var baseCommit: CommitInfo? = null

    /**
     * True if collapsed commits were expanded but were automatically collapsed again
     * Used to enable collapse action
     */
    var isNestedCollapsed: Boolean = false

    @Synchronized
    internal fun addListener(listener: Listener) = listeners.add(listener)

    /**
     * Sets name of
     * branch and notifies
     * listeners
     */
    @Synchronized
    internal fun setName(name: String) {
        this.name = name
        listeners.forEach { it.onNameChange(name) }
    }

    /**
     * Sets commits of
     * branch and notifies
     * listeners
     */
    @Synchronized
    internal fun setCommits(commits: List<CommitInfo>) {
        this.initialCommits = commits
        this.currentCommits = commits.toMutableList()
        collapseCommits()
        listeners.forEach { it.onCommitChange(commits) }
    }

    fun collapseCommits(
        initialIndex: Int = 5,
        finalIndex: Int = this.currentCommits.size - 2,
    ) {
        if (this.initialCommits.size < 7) return

        val collapsedCommits = this.currentCommits.subList(initialIndex, finalIndex).deepClonePolymorphic()
        val parentOfCollapsedCommit = this.currentCommits[finalIndex]
        collapseCommitsWithList(collapsedCommits, parentOfCollapsedCommit)
    }

    /**
     * Given a list of commits to be collapsed and a parent, collapses the commits.
     */
    fun collapseCommitsWithList(
        collapsedCommits: List<CommitInfo>,
        parentOfCollapsedCommit: CommitInfo,
    ) {
        val collapsedCommand = CollapseCommand(parentOfCollapsedCommit, collapsedCommits.toMutableList())

        parentOfCollapsedCommit.changes.add(collapsedCommand)
        parentOfCollapsedCommit.isCollapsed = true
        parentOfCollapsedCommit.isHovered = false

        this.currentCommits.removeAll(collapsedCommits)
        this.clearSelectedCommits()
        collapsedCommits.forEach {
            it.changes.add(collapsedCommand)
            it.isCollapsed = true
        }
    }

    /**
     * Adds to selected commits
     * of branch and notifies
     * listeners
     */
    @Synchronized
    internal fun addSelectedCommits(commit: CommitInfo) {
        selectedCommits.add(commit)
        listeners.forEach { it.onSelectedCommitChange(selectedCommits) }
    }

    /**
     * Removes from selected
     * commits of branch and
     * notifies listeners
     */
    @Synchronized
    internal fun removeSelectedCommits(commit: CommitInfo) {
        selectedCommits.remove(commit)
        commit.getChangesAfterPick().forEach {
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
    @Synchronized
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
    @Synchronized
    internal fun updateCurrentCommits(
        oldIndex: Int,
        newIndex: Int,
        commit: CommitInfo,
    ) {
        currentCommits.removeAt(oldIndex)
        currentCommits.add(newIndex, commit)
        listeners.forEach { it.onCurrentCommitsChange(currentCommits) }
    }

    @Synchronized
    internal fun addCommitsToCurrentCommits(
        index: Int,
        commits: List<CommitInfo>,
    ) {
        currentCommits.addAll(index, commits)
        listeners.forEach { it.onCurrentCommitsChange(currentCommits) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BranchInfo

        if (name != other.name) return false
        if (initialCommits != other.initialCommits) return false
        if (selectedCommits != other.selectedCommits) return false
        if (isPrimary != other.isPrimary) return false
        if (currentCommits != other.currentCommits) return false

        return true
    }

    /**
     * Gets the index of the current commit
     * taking into account squashed commits
     */
    internal fun indexOfCommit(commit: CommitInfo): Int {
        var ret = currentCommits.indexOf(commit)

        if (commit.isSquashed) {
            commit.getChangesAfterPick().forEach {
                if (it is FixupCommand) {
                    ret = currentCommits.indexOf(it.parentCommit) - 1
                } else if (it is SquashCommand) {
                    ret = currentCommits.indexOf(it.parentCommit) - 1
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
