package com.jetbrains.interactiveRebase.visuals

import com.intellij.ui.components.JBPanel
import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.event.ActionEvent
import javax.swing.AbstractAction

class HelpPanel() : JBPanel<JBPanel<*>>() {
//    var desktop: Desktop? = if (Desktop.isDesktopSupported() && !GraphicsEnvironment.isHeadless()) Desktop.getDesktop() else null

    /**
     * Secondary constructor for testing
     */
    constructor(desktop: Desktop) : this() {
//        this.desktop = desktop
    }

    init {
        this.layout = BorderLayout()
//        if (desktop != null) {
//            val action: Action = MyHelpAction(desktop!!)
//            val help = JButton(action)
//            help.toolTipText = "Show help contents"
//            help.putClientProperty("JButton.buttonType", "help")
//            this.add(help, BorderLayout.SOUTH)
//        }
    }

    class MyHelpAction(private var desktop: Desktop) : AbstractAction() {
        override fun actionPerformed(e: ActionEvent?) {
            // TODO: Change the link to the readme accessible by everyone
//            val uri =
//                URI(
//                    "https://gitlab.ewi.tudelft.nl/cse2000-software-project/2023-2024/cluster-p/12c" +
//                        "/interactive-rebase-jetbrains/-/blob/main/README.md",
//                )
//            desktop.browse(uri)
        }
    }
}
