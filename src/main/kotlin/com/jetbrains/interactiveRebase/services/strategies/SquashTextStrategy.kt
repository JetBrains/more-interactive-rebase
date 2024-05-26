package com.jetbrains.interactiveRebase.services.strategies

import com.jetbrains.interactiveRebase.dataClasses.commands.RebaseCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import java.awt.TextField

class SquashTextStrategy(private val command : SquashCommand, private val textField: TextField) : TextFieldStrategy {
    override fun handleEnter() {
        command.newMessage = textField.text
    }
}