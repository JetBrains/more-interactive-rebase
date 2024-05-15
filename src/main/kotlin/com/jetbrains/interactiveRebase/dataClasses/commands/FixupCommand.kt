package com.jetbrains.interactiveRebase.dataClasses.commands

import com.jetbrains.interactiveRebase.dataClasses.CommitInfo

data class FixupCommand(private val parentCommit: CommitInfo, private val fixupCommits: List<CommitInfo>) : RebaseCommand(parentCommit) {
    override fun execute() {
        TODO("Not yet implemented")
    }

    override fun undo() {
        TODO("Not yet implemented")
    }
}
