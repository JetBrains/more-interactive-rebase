package com.jetbrains.interactiveRebase.services.strategies

import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.RewordCommand
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import javax.swing.JTextField

class RewordTextStrategy(
    val commitInfo: CommitInfo,
    private val invoker: RebaseInvoker,
    private val textField: JTextField,
) : TextFieldStrategy {
    /**
     * Adds a reword change to the list of changes of a commit
     */
    override fun handleEnter() {
        if (commitInfo.commit.subject == textField.text && commitInfo.changes.filter { it is RewordCommand }.isEmpty()) return
        val command = RewordCommand(commitInfo, textField.text)
        commitInfo.addChange(command)
        invoker.addCommand(command)
    }
}
