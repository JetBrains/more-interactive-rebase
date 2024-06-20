package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.commands.CherryCommand
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener

class IRRepositoryChangeListener(val project: Project) : GitRepositoryChangeListener {

    /**
     * Refreshes the model whenever a change happens in the repository,
     * such as a commit, rebase, merge, etc.
     */
    override fun repositoryChanged(repository: GitRepository) {
        val invoker = project.service<RebaseInvoker>()
        if(invoker.commands.filterIsInstance<CherryCommand>().isNotEmpty()){
            if(project.service<ModelService>().noMoreCherryPicking){
                if (repository.isRebaseInProgress) {
                    project.service<ModelService>().refreshModelDuringRebaseProcess(repository.root)
                } else {
                    project.service<ModelService>().refreshModel()
                }
            }
//            if(!project.service<ModelService>().noMoreCherryPicking){
//                val leftToCherryPick = invoker.commands.filterIsInstance<CherryCommand>().size -
//                        project.service<ModelService>().counterForCherry
//                while(!project.service<ModelService>().isDoneCherryPicking){
//
//                }
//                if(leftToCherryPick==0 && project.service<ModelService>().isDoneCherryPicking){
//                    project.service<ModelService>().counterForCherry = 0
//                    project.service<ModelService>().noMoreCherryPicking = true
//                    invoker.executeCommands()
//                }
//            }else{
//                if (repository.isRebaseInProgress) {
//                    project.service<ModelService>().refreshModelDuringRebaseProcess(repository.root)
//                } else {
//                    project.service<ModelService>().refreshModel()
//                }
//            }

//            val head = GitUtil.getHead(repository)
//            if(IRGitUtils(project).isCherryPickInProcess(repository.root)){
//                println("Cherry picking commit " + IRGitUtils(project).getCurrentCherryPickCommit(repository.root))
//            }
//            else if(project.service<ModelService>().branchInfo.initialCommits.map{it -> it.commit.id}.contains(head).not()
//                    && project.service<ModelService>().cherryPickInProcess){
               // project.service<ModelService>().cherryPickInProcess = false
//                while(!project.service<ModelService>().isDoneCerryPicking){
//                }
                //invoker.executeCommands()
//            }
//            else {
//                println("cherry pick is aborted")
//                //project.service<ModelService>().fetchGraphInfo(0)
//            }

        }else{
            if (repository.isRebaseInProgress) {
                project.service<ModelService>().refreshModelDuringRebaseProcess(repository.root)
            } else {
                project.service<ModelService>().refreshModel()
            }
        }

    }
}
