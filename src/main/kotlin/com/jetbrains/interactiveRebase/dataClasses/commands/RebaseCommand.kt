package com.jetbrains.interactiveRebase.dataClasses.commands

import com.jetbrains.interactiveRebase.dataClasses.CommitInfo

sealed class RebaseCommand(private val commit: CommitInfo) {
    /**
     * This method is to set-up connection with the
     * Interactive Rebase mechanism.
     *
     * This will be called within the RebaseInvoker,
     * once the actual rebase is initiated through the rebase button.
     */
    abstract fun execute()

    /**
    Usually in a command you would need an undo() method,
    this is to be discussed if needed in the future.

     TODO: Discuss if undo() method is needed.
     **/
    abstract fun undo()
}