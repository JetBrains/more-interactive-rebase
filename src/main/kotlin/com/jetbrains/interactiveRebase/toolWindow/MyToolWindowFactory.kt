package com.jetbrains.interactiveRebase.toolWindow

import Circle
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.jetbrains.interactiveRebase.MyBundle
import com.jetbrains.interactiveRebase.components.Node
import com.jetbrains.interactiveRebase.services.MyProjectService
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import javax.swing.JButton

class MyToolWindowFactory : ToolWindowFactory {
    init {
        thisLogger().warn(
            "Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.",
        )
    }

    override fun createToolWindowContent(
        project: Project,
        toolWindow: ToolWindow,
    ) {
        val myToolWindow = MyToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(myToolWindow.getContent(), null, false)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class MyToolWindow(toolWindow: ToolWindow) {
        private val service = toolWindow.project.service<MyProjectService>()


        fun getContent() =
            JBPanel<JBPanel<*>>().apply {

                val node = Node()
                node.withPreferredSize(200,200)
                node.withMinimumHeight(100)
                node.withMinimumWidth(100)
                val c = Circle()
                c.preferredSize = Dimension(200, 200)

               // add(node)
                add(c, BorderLayout.CENTER)
//                val label = JBLabel(MyBundle.message("randomLabel", "?"))
//
//                add(label)
//                add(
//                    JButton(MyBundle.message("shuffle")).apply {
//                        addActionListener {
//                            label.text = MyBundle.message("randomLabel", service.getRandomNumber())
//                        }
//                    },
//                )
            }
    }
}
