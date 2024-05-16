package com.jetbrains.interactiveRebase.dataClasses

data class BranchInfo(var name: String = "", var commits: List<CommitInfo> = mutableListOf(), val isCheckedOut: Boolean = false) {
    var selectedCommits: MutableList<CommitInfo> = mutableListOf()
}
