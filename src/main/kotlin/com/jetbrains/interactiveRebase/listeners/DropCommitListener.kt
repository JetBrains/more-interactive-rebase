package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JButton

class DropCommitListener(
    val modelService: ModelService,
    button: JButton,
    val project: Project,
    private val invoker: RebaseInvoker,
) : MouseListener {
    constructor(button: JButton, project: Project, invoker: RebaseInvoker) :
        this(project.service<ModelService>(), button, project, invoker)

    /**
     * When the button is clicked, the selected commits are dropped.
     */
    override fun mouseClicked(e: MouseEvent?) {
//        val commits = modelService.getSelectedCommits()
//        commits.forEach {
//                commitInfo ->
//            commitInfo.addChange(DropCommand(mutableListOf(commitInfo), project))
//        }
//        invoker.addCommand(DropCommand(commits, project))
//        modelService.branchInfo.clearSelectedCommits()
    }

    /**
     * When the button is pressed, the selected commits are dropped.
     * (same behavior as mouseClicked)
     **/
    override fun mousePressed(e: MouseEvent?) {
        mouseClicked(e)
    }

    /**
     * Does nothing when the mouse is released.
     */
    override fun mouseReleased(e: MouseEvent?) {
    }

    /**
     * Does nothing when the mouse enters the button.
     */
    override fun mouseEntered(e: MouseEvent?) {
    }

    /**
     * Does nothing when the mouse exits the button.
     */
    override fun mouseExited(e: MouseEvent?) {
    }
}
