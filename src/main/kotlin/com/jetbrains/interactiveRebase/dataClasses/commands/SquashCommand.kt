package com.jetbrains.interactiveRebase.dataClasses.commands

import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import git4ideaClasses.GitRebaseEntryGeneratedUsingLog
import git4ideaClasses.IRGitModel

data class SquashCommand(
    val parentCommit: CommitInfo,
    val squashedCommits: List<CommitInfo>,
    val newMessage: String,
) :
    RebaseCommand(parentCommit) {
    override fun execute(model: IRGitModel<GitRebaseEntryGeneratedUsingLog>, branchInfo : BranchInfo) {
        TODO("Not yet implemented")
    }
}
