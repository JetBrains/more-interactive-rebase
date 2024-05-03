package com.jetbrains.interactiveRebase

import git4idea.GitCommit

abstract class CommitConsumer : com.intellij.util.Consumer<GitCommit> {
     abstract val commits: List<GitCommit>
     var commitCounter = 0
     var commitConsumptionCap : Int = 25

}