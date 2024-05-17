package com.jetbrains.interactiveRebase.dataClasses.commands

import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import git4ideaClasses.GitRebaseEntryGeneratedUsingLog
import git4ideaClasses.IRGitModel

// TODO: Add needed attributes for the indices or ordering of the commits, based on drag-and-drop implementation
data class ReorderCommand(override var commit: CommitInfo) : RebaseCommand(commit) {
    override fun execute(model: IRGitModel<GitRebaseEntryGeneratedUsingLog>, branchInfo : BranchInfo) {
        TODO("Not yet implemented")
    }

}
