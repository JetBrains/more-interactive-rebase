package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import java.awt.event.MouseEvent
import java.awt.event.MouseListener

class ResetButtonListener(
    val project: Project,
    private val invoker: RebaseInvoker,
) : MouseListener {

    override fun mouseClicked(e: MouseEvent?) {
        invoker.commands = mutableListOf()
        val currentBranchInfo = project.service<RebaseInvoker>().branchInfo
        println("mouse clicked or pressed")
        currentBranchInfo.currentCommits = currentBranchInfo.commits.toMutableList()
        currentBranchInfo.commits.forEach{
                commitInfo -> commitInfo.changes.clear()
        }
        currentBranchInfo.clearSelectedCommits()
    }

    override fun mousePressed(e: MouseEvent?) {
        mouseClicked(e)
    }

    override fun mouseReleased(e: MouseEvent?) {
        println("mouse released")
    }

    override fun mouseEntered(e: MouseEvent?) {
        println("mouse entered")
    }

    override fun mouseExited(e: MouseEvent?) {
        println("mouse exited")
    }
}
