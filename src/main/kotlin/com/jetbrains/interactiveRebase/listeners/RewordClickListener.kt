package com.jetbrains.interactiveRebase.listeners

import com.intellij.ui.JBColor
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.BorderFactory
import javax.swing.JComponent

class RewordClickListener(private val wrappedLabel : JComponent) : MouseListener {
//    override fun actionPerformed(e: ActionEvent?) {
//        print("clicked label $wrappedLabel")
//        TODO("Not yet implemented")
//    }

    override fun mouseClicked(e: MouseEvent?) {
        println("ENTERED")
        wrappedLabel.border = BorderFactory.createLineBorder(JBColor.MAGENTA)
        if (e != null) {
            if (e.clickCount >= 2) {
                println("aaaaa $wrappedLabel")
                wrappedLabel.border = BorderFactory.createLineBorder(JBColor.GREEN)
            }

        }
    }

    override fun mousePressed(e: MouseEvent?) {
        println("pressed")
        wrappedLabel.border = BorderFactory.createLineBorder(JBColor.GREEN)
        throw UnsupportedOperationException("mousePressed is not supported for the RewordListener $wrappedLabel")
    }

    override fun mouseReleased(e: MouseEvent?) {
        throw UnsupportedOperationException("mouseR is not supported for the RewordListener")
    }

    override fun mouseEntered(e: MouseEvent?) {
        throw UnsupportedOperationException("mouseEntered is not supported for the RewordListener" + wrappedLabel.name + "sfbsg")
    }

    override fun mouseExited(e: MouseEvent?) {
        throw UnsupportedOperationException("mousePressed is not supported for the RewordListener" +  wrappedLabel.name + "sfbsg")
    }
}