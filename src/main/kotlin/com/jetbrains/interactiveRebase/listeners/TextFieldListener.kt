package com.jetbrains.interactiveRebase.listeners

import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.RewordCommand
import com.jetbrains.interactiveRebase.visuals.RoundedTextField
import java.awt.event.KeyEvent
import java.awt.event.KeyListener

class TextFieldListener(private val commitInfo: CommitInfo, private val textField: RoundedTextField) : KeyListener {
    override fun keyTyped(e: KeyEvent?) {}

    override fun keyPressed(e: KeyEvent?) {}

    /**
     * If you click enter, you go through with rewording and exit the text box, if you click escape,
     * you close the text box without making any changes
     */
    override fun keyReleased(e: KeyEvent?) {
        val key = e?.keyCode
        when (key) {
            KeyEvent.VK_ENTER -> makeRewordChange()
            KeyEvent.VK_ESCAPE -> textField.exitTextBox()
        }
    }

    /**
     * Adds a reword change to the list of changes of a commit
     */
    private fun makeRewordChange() {
        commitInfo.changes.add(RewordCommand(commitInfo, textField.text))
        textField.exitTextBox()
    }
}
