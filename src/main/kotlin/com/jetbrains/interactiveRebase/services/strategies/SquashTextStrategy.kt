package com.jetbrains.interactiveRebase.services.strategies

import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import javax.swing.JTextField

class SquashTextStrategy(
    private val command: SquashCommand,
    private val textField: JTextField,
) : TextFieldStrategy {
    override fun handleEnter() {
        command.newMessage = textField.text
    }
}
