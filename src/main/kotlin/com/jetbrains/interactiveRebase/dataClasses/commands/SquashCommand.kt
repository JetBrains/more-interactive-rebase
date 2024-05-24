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
    val squashedCommits: List<CommitInfo>,
    val newMessage: String,
) :
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
        val commitIndices = squashedCommits.map { commit -> branchInfo.currentCommits.reversed().indexOf(commit) }
        val uniteRoot = model.unite(commitIndices)
        model.reword(uniteRoot.index, newMessage)

//        val repo = GitUtil.getRepositoryManager(project).getRepositoryForRootQuick(project.guessProjectDir())
//        if (repo != null) {
//            IRGitEditorHandler(repo, model).processModel(model) { entry ->
//                squashedCommits.find { it.commit.id.asString().startsWith(entry.commit) }?.commit.fullMessage

//                        ?: throw IllegalStateException("Full message should be taken from reworded commits only")
//            }
//        }

    }
}
