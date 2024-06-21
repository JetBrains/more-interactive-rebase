package com.jetbrains.interactiveRebase.dataClasses.commands

import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import git4ideaClasses.GitRebaseEntryGeneratedUsingLog
import git4ideaClasses.IRGitModel

data class CollapseCommand(var firstCommit: CommitInfo, val collapsedCommits: MutableList<CommitInfo>) : IRCommand() {
    override fun execute(
        model: IRGitModel<GitRebaseEntryGeneratedUsingLog>,
        branchInfo: BranchInfo,
    ) {
    }

    override fun commitOfCommand(): CommitInfo {
        return firstCommit
    }
}
