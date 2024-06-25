package com.jetbrains.interactiveRebase.visuals

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.services.DialogService
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JButton

class HelpPanel(project: Project) : JBPanel<JBPanel<*>>() {
    init {
        this.layout = BorderLayout()
        val action: Action = MyHelpAction(project)
        val help = JButton(action)
        help.toolTipText = "Show help contents"
        help.putClientProperty("JButton.buttonType", "help")
        this.add(help, BorderLayout.SOUTH)
    }

    class MyHelpAction(private val project: Project) : AbstractAction() {
        override fun actionPerformed(e: ActionEvent?) {
            // TODO: Change the link to the readme accessible by everyone
            val site =
                "https://gitlab.ewi.tudelft.nl/cse2000-software-project/2023-2024/cluster-p/" +
                    "12c/interactive-rebase-jetbrains/-/blob/main/README.md?ref_type=heads#-quick-start"
            val url = BrowserUtil.getURL(site)
            if (url != null) {
                BrowserUtil.browse(url)
            } else {
                project.service<DialogService>().warningOkCancelDialog("Error Accessing Help Page", "Help page could not be accessed.")
            }
        }
    }
}
