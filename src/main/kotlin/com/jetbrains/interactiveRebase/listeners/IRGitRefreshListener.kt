package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.services.ModelService
import git4idea.repo.GitRepository
import git4idea.status.GitRefreshListener

class IRGitRefreshListener(private val project: Project) : GitRefreshListener {
    /**
     * Fetches the branch info
     * when the repository is updated
     */
    override fun repositoryUpdated(repository: GitRepository) {
        project.service<ModelService>().fetchGraphInfo()
    }
}
