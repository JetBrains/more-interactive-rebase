package com.jetbrains.interactiveRebase.services.strategies

import com.jetbrains.interactiveRebase.dataClasses.commands.RebaseCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import java.awt.TextField
import javax.swing.JTextField

class SquashTextStrategy(private val command : SquashCommand, private val textField: JTextField) : TextFieldStrategy {
    override fun handleEnter() {
        println("strategy used is squash")
        command.newMessage = textField.text
        println("new message is ${command.newMessage}")
    }
}