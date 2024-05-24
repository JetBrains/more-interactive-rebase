package com.jetbrains.interactiveRebase.actions.finalPanel

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.JButton

class ResetAction: AnAction("Reset", "Resets all changes for the rebase",  JButton("Reset").icon) {
    override fun actionPerformed(e: AnActionEvent) {
        println("reset")
        TODO("Not yet implemented")
    }


}