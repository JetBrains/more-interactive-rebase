package com.jetbrains.interactiveRebase.dataClasses.commands

import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import git4ideaClasses.GitRebaseEntryGeneratedUsingLog
import git4ideaClasses.IRGitModel

data class FixupCommand(var parentCommit: CommitInfo, val fixupCommits: MutableList<CommitInfo>) :
    RebaseCommand() {
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
        val fixups = fixupCommits + parentCommit
        val commitIndices =
            fixups.map {
                    c ->
                branchInfo.currentCommits.reversed().indexOf(c)
            }.reversed()
        model.unite(commitIndices)
    }

    override fun toString(): String {
        return "FixupCommand(${parentCommit.commit.subject})"
    }
}
