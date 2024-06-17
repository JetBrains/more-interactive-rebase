package com.jetbrains.interactiveRebase.dataClasses.commands

import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import git4ideaClasses.GitRebaseEntryGeneratedUsingLog
import git4ideaClasses.IRGitModel

class RebaseCommand(val commit: CommitInfo) :
    IRCommand() {
    /**
     * This method is to set up connection with the
     * Interactive Rebase mechanism.
     *
     * This will be called within the RebaseInvoker,
     * once the actual rebase is initiated through the rebase button.
     */

    override fun execute(
        model: IRGitModel<GitRebaseEntryGeneratedUsingLog>,
        branchInfo: BranchInfo,
    ) {
    }

    override fun commitOfCommand(): CommitInfo {
        return commit
    }
}
