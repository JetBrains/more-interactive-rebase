package com.jetbrains.interactiveRebase.utils.consumers

import git4idea.GitCommit

abstract class CommitConsumer : com.intellij.util.Consumer<GitCommit> {
    abstract val commits: List<GitCommit>
}
