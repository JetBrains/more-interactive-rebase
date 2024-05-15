package com.jetbrains.interactiveRebase.dataClasses.commands

import com.jetbrains.interactiveRebase.dataClasses.CommitInfo

class ReorderCommand(private val commit: CommitInfo) : RebaseCommand(commit) {
    override fun execute() {
        TODO("Not yet implemented")
    }

    override fun undo() {
        TODO("Not yet implemented")
    }
}
