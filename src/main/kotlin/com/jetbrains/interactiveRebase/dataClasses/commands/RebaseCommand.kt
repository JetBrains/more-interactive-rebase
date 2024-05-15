package com.jetbrains.interactiveRebase.dataClasses.commands

import com.jetbrains.interactiveRebase.dataClasses.CommitInfo

/**
 * This is the abstract class for all the commands
 * that will be executed during the rebase process.
 *
 * This class is the base class for all the commands
 * that will be executed during the rebase process.
 *
 * @param commit The commit that the command will be executed on.
 */
sealed class RebaseCommand(open var commit: CommitInfo) {
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
