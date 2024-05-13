package com.jetbrains.interactiveRebase

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry
import com.jetbrains.interactiveRebase.utils.gitUtils.IRGitRebaseUtils
import com.jetbrains.interactiveRebase.utils.gitUtils.IRGitUtils
import git4idea.i18n.GitBundle

internal class GitInteractiveRebaseAction(private val project : Project) : GitSingleCommitEditingAction() {
    override val prohibitRebaseDuringRebasePolicy = ProhibitRebaseDuringRebasePolicy.Prohibit(
            GitBundle.message("rebase.log.action.operation.rebase.name")
    )

    override fun actionPerformedAfterChecks(commitEditingData: SingleCommitEditingData) {
//        val commit = commitEditingData.selectedCommit
//        val repository = commitEditingData.repository
//
//        if (Registry.`is`("git.interactive.rebase.collect.entries.using.log")) {
//            IRGitRebaseUtils(project).interactivelyRebaseUsingLog(commit, commitEditingData.logData)
//        }
//        else {
//            IRGitRebaseUtils(project).startInteractiveRebase(commit)
//        }
    }

    override fun getFailureTitle(): String = GitBundle.message("rebase.log.interactive.action.failure.title")
}