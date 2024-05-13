package com.jetbrains.interactiveRebase.dataClasses.commands

import com.jetbrains.interactiveRebase.dataClasses.CommitInfo

sealed class RebaseCommand(private val commit: CommitInfo) {
    abstract fun execute()
    abstract fun undo()
}