package com.jetbrains.interactiveRebase.dataClasses.commands

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.utils.gitUtils.IRGitEditorHandler
import git4idea.GitUtil
import git4ideaClasses.GitRebaseEntryGeneratedUsingLog
import git4ideaClasses.IRGitModel

data class SquashCommand(


    val parentCommit: CommitInfo,
    val squashedCommits: MutableList<CommitInfo>,
    val newMessage: String,

) :
    RebaseCommand() {

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

        squashedCommits.add(parentCommit)
        val commitIndices =  squashedCommits.map{
            c-> branchInfo.currentCommits.reversed().indexOf(c)}.reversed()
        model.unite(commitIndices)
        //model.reword(branchInfo.currentCommits.reversed().indexOf(parentCommit), newMessage)
//        val repo = GitUtil.getRepositoryManager(project).getRepositoryForRootQuick(project.guessProjectDir())
  //      if = null) {
//            IRGitEditorHandler(repo, model).processModel(model) { entry ->
//                squashedCommits.find { it.commit.id.asString().startsWith(entry.commit) }?.commit.fullMessage

//                        ?: throw IllegalStateException("Full message should be taken from reworded commits only")
//            }
//        }
        //This existed in the previous functionality but could make it work

    }



    }

