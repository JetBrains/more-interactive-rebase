package com.jetbrains.interactiveRebase.dataClasses.commands

import com.intellij.openapi.project.Project
import git4idea.rebase.GitRebaseProcess
import git4idea.rebase.GitRebaseResumeMode
import git4idea.rebase.GitRebaseSpec

class Bla(project: Project, rebaseSpec: GitRebaseSpec, customMode: GitRebaseResumeMode?) : GitRebaseProcess(project, rebaseSpec, customMode, ) {
}