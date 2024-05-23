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
    var isReordered: Boolean = false,
    var isDragged: Boolean = false,
) {
    internal val listeners: MutableList<Listener> = mutableListOf()

    /**
     * Adds a subscriber
     * to list of listeners
     */

    fun addListener(listener: Listener) = listeners.add(listener)

    /**
     * Adds a change to the
     * list of changes of the
     * commit
     */
    fun addChange(change: RebaseCommand) {
        changes.add(change)
        listeners.forEach { it.onCommitChange() }
    }

    /**
     * Sets isSelected to
     * passed value
     */
    fun setSelectedTo(value: Boolean) {
        isSelected = value
    }

    /**
     * Sets isHovered to
     * passed value
     */
    fun setHoveredTo(value: Boolean) {
        isHovered = value
    }

    /**
     * Sets isDoubleClicked to
     * passed value and notifies
     * subscribers
     */
    fun setDoubleClickedTo(value: Boolean) {
        isDoubleClicked = value
        listeners.forEach { it.onCommitChange() }
    }

    /**
     * Sets whether circle is reordered.
     */
    fun setReorderedTo(value: Boolean) {
        isReordered = value
        listeners.forEach { it.onCommitChange() }
    }

    /**
     * Sets whether circle is dragged.
     */
    fun setDraggedTo(value: Boolean) {
        isDragged = value
    }

    /**
     * Toggles isSelected
     */

    fun flipSelected() {
        isSelected = !isSelected
    }

    /**
     * Toggles isHovered
     */

    fun flipHovered() {
        isHovered = !isHovered
    }

    /**
     * Toggles isDoubleClicked
     * and notifies subscribers
     */
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
