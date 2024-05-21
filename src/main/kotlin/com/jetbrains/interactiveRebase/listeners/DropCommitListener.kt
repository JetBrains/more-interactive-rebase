package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.services.ComponentService
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JButton

class DropCommitListener(val button: JButton, val project: Project) : MouseListener {
    /**
     * When the button is clicked, the selected commits are dropped.
     */
    override fun mouseClicked(e: MouseEvent?) {
        val service = project.service<ComponentService>()
        service.getSelectedCommits().forEach {
                commitInfo ->
            commitInfo.changes.add(DropCommand(commitInfo))

            commitInfo.isSelected = false
            service.branchInfo.selectedCommits.remove(commitInfo)
        }

        service.isDirty = true
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
