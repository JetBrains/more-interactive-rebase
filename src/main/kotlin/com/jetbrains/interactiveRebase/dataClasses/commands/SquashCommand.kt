package com.jetbrains.interactiveRebase.dataClasses.commands

import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import git4ideaClasses.GitRebaseEntryGeneratedUsingLog
import git4ideaClasses.IRGitModel

data class SquashCommand(
    val parentCommit: CommitInfo,
    val squashedCommits: MutableList<CommitInfo>,
    var newMessage: String,
) :
    IRCommand() {
    /**
     * This method is  set up connection with the
     * Interactive Rebase mechanism.
     *
     * This will be called within the RebaseInvoker,
     * once the actual rebase is initiated through the rebase button.
     */
    override fun execute(
        model: IRGitModel<GitRebaseEntryGeneratedUsingLog>,
        branchInfo: BranchInfo,
    ) {
        val squashes = squashedCommits + parentCommit
        val commitIndices =
            squashes.map {
                    c ->
                branchInfo.currentCommits.reversed().indexOf(c)
            }.reversed()
        model.unite(commitIndices)
        model.reword(branchInfo.currentCommits.reversed().indexOf(parentCommit), newMessage)
    }

    override fun commitOfCommand(): CommitInfo {
        return parentCommit
    }
}
