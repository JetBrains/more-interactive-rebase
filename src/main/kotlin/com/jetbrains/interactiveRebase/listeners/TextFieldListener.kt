package com.jetbrains.interactiveRebase.listeners

import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import com.jetbrains.interactiveRebase.services.strategies.RewordTextStrategy
import com.jetbrains.interactiveRebase.services.strategies.TextFieldStrategy
import com.jetbrains.interactiveRebase.visuals.RoundedTextField
import java.awt.event.KeyEvent
import java.awt.event.KeyListener

class TextFieldListener(
    commitInfo: CommitInfo,
    private val textField: RoundedTextField,
    invoker: RebaseInvoker,
) : KeyListener {
    /**
     * Set to reword by default, sets the logic to be executed once pressing enter in a text field
     */
    var strategy: TextFieldStrategy = RewordTextStrategy(commitInfo, invoker, textField)

    override fun keyTyped(e: KeyEvent?) {}

    override fun keyPressed(e: KeyEvent?) {}

    /**
     * If you click enter, you go through with rewording and exit the text box, if you click escape,
     * you close the text box without making any changes
     */
    override fun keyReleased(e: KeyEvent?) {
        val key = e?.keyCode
        when (key) {
            KeyEvent.VK_ENTER -> processEnter()
            KeyEvent.VK_ESCAPE -> textField.exitTextBox()
        }
    }

    private fun processEnter() {
        strategy.handleEnter()
        textField.exitTextBox()
    }
}
