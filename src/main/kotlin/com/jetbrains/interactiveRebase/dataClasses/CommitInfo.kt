package com.jetbrains.interactiveRebase.dataClasses

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.commands.IRCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.PickCommand
import git4idea.GitCommit

data class CommitInfo(
    var commit: GitCommit,
    val project: Project,
    val changes: MutableList<IRCommand> = mutableListOf(),
    var isSelected: Boolean = false,
    var isHovered: Boolean = false,
    var isTextFieldEnabled: Boolean = false,
    var isSquashed: Boolean = false,
    var isReordered: Boolean = false,
    var isDragged: Boolean = false,
    var isCollapsed: Boolean = false,
    var isPaused: Boolean = false,
    var isRebased: Boolean = false,
    var wasCherryPicked: Boolean = false,
) {
    internal val listeners: MutableList<Listener> = mutableListOf()

    /**
     * Adds a subscriber
     * to list of listeners
     */

    @Synchronized
    fun addListener(listener: Listener) = listeners.add(listener)

    /**
     * Adds a change to the
     * list of changes of the
     * commit
     */
    @Synchronized
    fun addChange(change: IRCommand) {
        changes.add(change)
        listeners.forEach { it.onCommitChange() }
    }

    /**
     * Removes a change to the
     * list of changes of the
     * commit
     */
    @Synchronized
    fun removeChange(change: IRCommand) {
        changes.asReversed().removeIf { it === change }
        listeners.forEach { it.onCommitChange() }
    }

    /**
     * Sets isTextFieldEnabled to
     * passed value and notifies
     * subscribers
     */
    @Synchronized
    fun setTextFieldEnabledTo(value: Boolean) {
        isTextFieldEnabled = value
        listeners.forEach { it.onCommitChange() }
    }

    /**
     * Sets whether circle is reordered.
     */
    @Synchronized
    fun setReorderedTo(value: Boolean) {
        isReordered = value
        listeners.forEach { it.onCommitChange() }
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
    @Synchronized
    fun flipDoubleClicked() {
        isTextFieldEnabled = !isTextFieldEnabled
        listeners.forEach { it.onCommitChange() }
    }

    fun getChangesAfterPick(): List<IRCommand> {
        val indexOfPick = changes.indexOfLast { it is PickCommand }
        return if (indexOfPick != -1) {
            changes.subList(indexOfPick, changes.size)
        } else {
            changes
        }
    }

    @Synchronized
    fun markAsPaused() {
        isPaused = true
        listeners.forEach { it.onCommitChange() }
    }

    @Synchronized
    fun markAsNotPaused() {
        isPaused = false
        listeners.forEach { it.onCommitChange() }
    }

    @Synchronized
    fun markAsRebased() {
        isRebased = true
        listeners.forEach { it.onCommitChange() }
    }

    /**
     * Shows commitInfo in a human-readable way
     */
    override fun toString(): String {
        return "CommitInfo(commit=${commit.subject})"
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
