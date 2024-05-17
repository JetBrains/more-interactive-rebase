package com.jetbrains.interactiveRebase.listeners.reword

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.services.ComponentService
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.BorderFactory
import javax.swing.JComponent

class RewordClickListener(private val commitInfo : CommitInfo) : MouseListener {
    private val project = commitInfo.project
    private val compSer = project.service<ComponentService>()

    override fun mouseClicked(e: MouseEvent?) {
        println("ENTERED")
        if (e != null) {
            if (e.clickCount >= 2) {
                println("ooh 2 clicks")
                commitInfo.isDoubleClicked = true
                commitInfo.isSelected = true
                compSer.isDirty = true
            }
        }
    }

    override fun mousePressed(e: MouseEvent?) {
        println("PRESSED")
//        wrappedLabel.border = BorderFactory.createLineBorder(JBColor.GREEN)
//        throw UnsupportedOperationException("mousePressed is not supported for the RewordListener")
    }

    override fun mouseReleased(e: MouseEvent?) {
        println("released")
//        throw UnsupportedOperationException("mouseR is not supported for the RewordListener")
    }

    override fun mouseEntered(e: MouseEvent?) {
        println("entered")
//        throw UnsupportedOperationException("mouseEntered is not supported for the RewordListener $commitInfo sfbsg")
    }

    override fun mouseExited(e: MouseEvent?) {
        println("exit")
//        throw UnsupportedOperationException("mousePressed is not supported for the RewordListener sfbsg")
    }
}