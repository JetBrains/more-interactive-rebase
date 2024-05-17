package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.services.ComponentService
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.border.Border

class DropCommitListener(val button: JButton, val project: Project): MouseListener{
    private val originalBorder: Border = button.border
    private val originalBackground = button.foreground
    private val clickedBackground = button.foreground.brighter()
    private val clickedBorder: Border = BorderFactory.createLineBorder(JBColor.BLACK, 1, true)

    override fun mouseClicked(e: MouseEvent?) {
        val service = project.service<ComponentService>()
        button.border = clickedBorder
        button.foreground = clickedBackground
        button.isOpaque = true
        service.getSelectedCommits().forEach {
            commitInfo ->
            commitInfo.changes.add(DropCommand(commitInfo))

            commitInfo.isSelected = false
            service.branchInfo.selectedCommits.remove(commitInfo)
        }

        service.isDirty = true
    }

    override fun mousePressed(e: MouseEvent?) {
        mouseClicked(e)
    }

    override fun mouseReleased(e: MouseEvent?) {
        button.border = originalBorder
        button.foreground = originalBackground
        button.isOpaque = false
    }

    override fun mouseEntered(e: MouseEvent?) {
        button.border = clickedBorder
        button.foreground = clickedBackground
        button.isOpaque = true
        println("mouse entered")
    }

    override fun mouseExited(e: MouseEvent?) {
        button.border = originalBorder
        button.foreground = originalBackground
        button.isOpaque = false
    }

}