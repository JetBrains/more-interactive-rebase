package com.jetbrains.interactiveRebase.listeners.reword

import com.intellij.openapi.project.Project
import java.awt.event.FocusEvent
import java.awt.event.FocusListener

class RewordFocusListener(private val project : Project, ) : FocusListener {
    override fun focusGained(e: FocusEvent?) {
        println("gained focus")

    }

    override fun focusLost(e: FocusEvent?) {
        println("lost focus")
    }
}