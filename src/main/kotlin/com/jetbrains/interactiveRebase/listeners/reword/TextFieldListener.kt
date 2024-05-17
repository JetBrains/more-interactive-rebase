package com.jetbrains.interactiveRebase.listeners.reword

import com.intellij.diff.tools.util.KeyboardModifierListener
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.RebaseCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.RewordCommand
import com.jetbrains.interactiveRebase.services.ComponentService
import java.awt.TextField
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.JTextField

class TextFieldListener(private val commitInfo: CommitInfo, private val textField : JTextField) : KeyListener {
    private val project : Project = commitInfo.project
    private val componentService = project.service<ComponentService>()
    override fun keyTyped(e: KeyEvent?) {
        println("entered type")


//        TODO("Not yet implemented")
    }

    override fun keyPressed(e: KeyEvent?) {
        println("pressed $e")
    }

    override fun keyReleased(e: KeyEvent?) {
        println("key released $e")
        val key = e?.keyCode
        when (key) {
            KeyEvent.VK_ENTER -> makeRewordChange()
            KeyEvent.VK_ESCAPE -> exitTextBox()
        }
    }

    fun exitTextBox() {
        commitInfo.isDoubleClicked = false
        componentService.branchInfo.selectedCommits.remove(commitInfo)
        commitInfo.isSelected = false
        componentService.isDirty = true
    }

    fun makeRewordChange() {
        val changes : MutableList<RebaseCommand> = commitInfo.changes
        changes.add(RewordCommand(commitInfo, textField.text))
        exitTextBox()
    }
}