package com.jetbrains.interactiveRebase.dataClasses

import com.intellij.openapi.Disposable

class BranchInfo(
    var name: String = "",
    var initialCommits: List<CommitInfo> = listOf(),
    var selectedCommits: MutableList<CommitInfo> = mutableListOf(),
    val isCheckedOut: Boolean = false,
) {
    internal var currentCommits: MutableList<CommitInfo> = mutableListOf()
    private val listeners: MutableList<Listener> = mutableListOf()

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
        listeners.forEach { it.onSelectedCommitChange(selectedCommits) }
    }

    /**
     * Clears selected commits
     * of branch and notifies
     * listeners
     */
    internal fun clearSelectedCommits() {
        selectedCommits.forEach { it.setSelectedTo(false) }
        selectedCommits.clear()
        listeners.forEach { it.onSelectedCommitChange(selectedCommits) }
    }

    internal fun updateCurrentCommits(
        oldIndex: Int,
        newIndex: Int,
        commit: CommitInfo,
    ) {
        currentCommits.removeAt(oldIndex)
        currentCommits.add(newIndex, commit)
        listeners.forEach { it.onCurrentCommitsChange(currentCommits) }
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
