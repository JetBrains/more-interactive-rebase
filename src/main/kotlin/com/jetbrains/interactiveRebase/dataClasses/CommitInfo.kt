package com.jetbrains.interactiveRebase.dataClasses

import com.intellij.openapi.project.Project
import git4idea.GitCommit

class CommitInfo(
    val commit: GitCommit,
    val project: Project,
    // TODO: (figure out VISUAL CHANGES type)
    val changes: List<*>?,
    var isSelected: Boolean = false,
    var isHovered: Boolean = false,
) {
    fun getSubject(): String {
        return commit.subject
    }
}
