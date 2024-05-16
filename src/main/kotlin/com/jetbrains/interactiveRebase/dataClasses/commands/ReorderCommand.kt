package com.jetbrains.interactiveRebase.dataClasses.commands

import com.jetbrains.interactiveRebase.dataClasses.CommitInfo

// TODO: Add needed attributes for the indices or ordering of the commits, based on drag-and-drop implementation
data class ReorderCommand(override var commit: CommitInfo) : RebaseCommand(commit) {
    override fun execute() {
        TODO("Not yet implemented")
    }

    override fun undo() {
        TODO("Not yet implemented")
    }
}
