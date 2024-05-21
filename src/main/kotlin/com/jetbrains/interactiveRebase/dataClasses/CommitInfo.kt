package com.jetbrains.interactiveRebase.dataClasses

import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.commands.RebaseCommand
import git4idea.GitCommit

data class CommitInfo(
    val commit: GitCommit,
    val project: Project,
    val changes: MutableList<RebaseCommand> = mutableListOf(),
    var isSelected: Boolean = false,
    var isHovered: Boolean = false,
) {
    fun getSubject(): String {
        return commit.subject
    }
}
