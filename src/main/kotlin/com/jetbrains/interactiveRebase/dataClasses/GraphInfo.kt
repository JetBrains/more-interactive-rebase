package com.jetbrains.interactiveRebase.dataClasses

data class GraphInfo(var mainBranch: BranchInfo, var addedBranch: BranchInfo? = null) {
    internal var branchList = mutableListOf<String>()
}
