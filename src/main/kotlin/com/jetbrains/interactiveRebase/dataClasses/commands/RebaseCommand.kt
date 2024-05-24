package com.jetbrains.interactiveRebase.dataClasses.commands

import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import git4ideaClasses.GitRebaseEntryGeneratedUsingLog
import git4ideaClasses.IRGitModel

/**
 * This is the abstract class for all the commands
 * that will be executed during the rebase process.
 *
 * This class is the base class for all the commands
 * that will be executed during the rebase process.
 *
 * @param commit The commit that the command will be executed on.
 */
sealed class RebaseCommand(open var commits: MutableList<CommitInfo>) {
    /**
     * This method is to set up connection with the
     * Interactive Rebase mechanism.
     *
     * This will be called within the RebaseInvoker,
     * once the actual rebase is initiated through the rebase button.
     */
    internal abstract fun execute(
        model: IRGitModel<GitRebaseEntryGeneratedUsingLog>,
        branchInfo: BranchInfo,
    )
}
