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
//    private val labeledBranchPanel = project.service<ComponentService>().getLabeledBranchPanel()
//    val selectedCircles: MutableList<CirclePanel> =  project.service<ComponentService>().getSelectedCirclePanels(labeledBranchPanel)

    override fun mouseClicked(e: MouseEvent?) {
        button.border = clickedBorder
        button.foreground = clickedBackground
        button.isOpaque = true
        project.service<ComponentService>().getSelectedCommits().forEach {
            commitInfo ->
            commitInfo.changes?.add(DropCommand(commitInfo))
            println("lolza")
        }
        project.service<ComponentService>().isDirty = true

//        for (i in selectedCircles.indices) {
//            val circle = selectedCircles[i]
//
//            val dropCirclePanel = circle as DropCirclePanel
//            selectedCircles[i] = dropCirclePanel
//
//        }

        println("mouse clicked")
    }

    override fun mousePressed(e: MouseEvent?) {
        button.border = clickedBorder
        button.foreground = clickedBackground
        button.isOpaque = true
        project.service<ComponentService>().getSelectedCommits().forEach {
            commitInfo ->
            commitInfo.changes?.add(DropCommand(commitInfo))
            println("kkkkkkadfkjbefshjgka")
        }
        project.service<ComponentService>().isDirty = true

        println("mouse pressed")
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