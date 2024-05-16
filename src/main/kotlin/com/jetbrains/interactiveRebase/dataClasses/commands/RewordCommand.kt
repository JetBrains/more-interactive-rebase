package com.jetbrains.interactiveRebase.dataClasses.commands

import com.jetbrains.interactiveRebase.dataClasses.CommitInfo

data class RewordCommand(override var commit: CommitInfo, var newMessage: String) : RebaseCommand(commit) {
    override fun execute() {
        TODO("Not yet implemented")
    }

    override fun undo() {
        TODO("Not yet implemented")
    }
}
