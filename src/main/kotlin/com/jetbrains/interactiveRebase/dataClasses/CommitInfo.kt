package com.jetbrains.interactiveRebase.dataClasses

import git4idea.GitCommit

class CommitInfo(
        val commit: GitCommit,
        val changes: List<*>?//TODO: (figure out VISUAL CHANGES type)
        , var isSelected: Boolean = false,
        var isHovered: Boolean = false,
      ){

      fun getSubject(): String {
        return commit.subject
      }

}