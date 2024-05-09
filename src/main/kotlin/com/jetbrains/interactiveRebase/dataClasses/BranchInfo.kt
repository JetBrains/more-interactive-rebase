package com.jetbrains.interactiveRebase.dataClasses

import git4idea.GitCommit

data class BranchInfo(var commits: MutableList<GitCommit>, var branchName: String)
