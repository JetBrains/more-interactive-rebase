package com.jetbrains.interactiveRebase.dataClasses.commands

import com.jetbrains.interactiveRebase.dataClasses.CommitInfo

data class FixupCommand(private val commit: CommitInfo, private val fixupCommits: List<CommitInfo>) {
}