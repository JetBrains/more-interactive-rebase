package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.services.ModelService
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener

class IRRepositoryChangeListener(val project: Project) : GitRepositoryChangeListener {
    /**
     * Refreshes the model whenever a change happens in the repository,
     * such as a commit, rebase, merge, etc.
     */
    override fun repositoryChanged(repository: GitRepository) {
        if (repository.isRebaseInProgress) {
            project.service<ModelService>().refreshModelDuringRebaseProcess(repository.root)
        } else {
            project.service<ModelService>().refreshModel()
        }
    }
}
