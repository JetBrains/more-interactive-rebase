package com.jetbrains.interactiveRebase.dataClasses.commands

import com.jetbrains.interactiveRebase.dataClasses.CommitInfo

data class SquashCommand(
    private val parentCommit: CommitInfo,
    private val squashedCommits: List<CommitInfo>,
    private val newMessage: String,
) :
    RebaseCommand(parentCommit) {
    override fun execute() {
        TODO("Not yet implemented")
    }

    override fun undo() {
        TODO("Not yet implemented")
    }
}
