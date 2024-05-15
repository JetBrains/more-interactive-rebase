package com.jetbrains.interactiveRebase.dataClasses.commands

import com.jetbrains.interactiveRebase.dataClasses.CommitInfo

data class SquashCommand(
    val parentCommit: CommitInfo,
    val squashedCommits: List<CommitInfo>,
    val newMessage: String,
) :
    RebaseCommand(parentCommit) {
    override fun execute() {
        TODO("Not yet implemented")
    }

    override fun undo() {
        TODO("Not yet implemented")
    }
}
