package com.jetbrains.interactiveRebase.dataClasses.commands

import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import git4ideaClasses.GitRebaseEntryGeneratedUsingLog
import git4ideaClasses.IRGitModel

data class RewordCommand(override var commit: CommitInfo, var newMessage: String) : RebaseCommand(commit) {
    override fun execute(model: IRGitModel<GitRebaseEntryGeneratedUsingLog>, branchInfo : BranchInfo) {
        model.pick(listOf(0))
    }

<<<<<<< HEAD
    /**
     Usually in a command you would need an undo() method,
     this is to be discussed if needed in the future.

     TODO: Discuss if undo() method is needed.
     **/
    override fun undo() {
        TODO("Not yet implemented")
    }
=======
>>>>>>> 797c8d3 (invoker created but connection doesnt work)
}
