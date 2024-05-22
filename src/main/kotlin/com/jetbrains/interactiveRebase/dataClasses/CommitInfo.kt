package com.jetbrains.interactiveRebase.dataClasses

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo.Listener
import com.jetbrains.interactiveRebase.dataClasses.commands.RebaseCommand
import git4idea.GitCommit

data class CommitInfo(
    val commit: GitCommit,
    val project: Project,
    val changes: MutableList<RebaseCommand> = mutableListOf(),
    var isSelected: Boolean = false,
    var isHovered: Boolean = false,
    var isDoubleClicked: Boolean = false,
) {
    internal val listeners: MutableList<Listener> = mutableListOf()

    /**
     * Adds a subscriber
     * to list of listeners
     */

    fun addListener(listener: Listener) = listeners.add(listener)

    fun addChange(change: RebaseCommand) {
        changes.add(change)
        listeners.forEach { it.onCommitChange() }
    }

    fun setSelectedTo(value: Boolean) {
        isSelected = value
    }

    fun setHoveredTo(value: Boolean) {
        isHovered = value
    }

    fun setDoubleClickedTo(value: Boolean) {
        isDoubleClicked = value
        listeners.forEach { it.onCommitChange() }
    }

    fun flipSelected() {
        isSelected = !isSelected
    }

    fun flipHovered() {
        isHovered = !isHovered
    }

    fun flipDoubleClicked() {
        isDoubleClicked = !isDoubleClicked
        listeners.forEach { it.onCommitChange() }
    }

    /**
     * Provides a listener
     * for changes in this class
     */
    interface Listener : Disposable {
        fun onCommitChange()

        override fun dispose() {}
    }
}
