package com.jetbrains.interactiveRebase.listeners.reword

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.services.ComponentService
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

class RewordButtonListener(private val project : Project) : ActionListener {
//    private val project : Project = commitInfo.project

    override fun actionPerformed(e: ActionEvent?) {
        val componentService : ComponentService = project.service<ComponentService>()
        componentService.branchInfo.selectedCommits.forEach {
//            println("in loop $it")
            it.isDoubleClicked = true
        }
        if (componentService.branchInfo.selectedCommits.isNotEmpty()) {
            componentService.isDirty = true
//            componentService.mainPanel.repaint()
        }

    }
}