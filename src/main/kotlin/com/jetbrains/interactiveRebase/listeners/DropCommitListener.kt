package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.services.ModelService
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JButton

class DropCommitListener(val modelService: ModelService, button: JButton, val project: Project) : MouseListener {
    constructor(button: JButton, project: Project) : this(project.service<ModelService>(), button, project)

    /**
     * When the button is clicked, the selected commits are dropped.
     */
    override fun mouseClicked(e: MouseEvent?) {
        modelService.getSelectedCommits().forEach {
                commitInfo ->
            commitInfo.addChange(DropCommand(commitInfo))
        }
        modelService.branchInfo.clearSelectedCommits()
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
