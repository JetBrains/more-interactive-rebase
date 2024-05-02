package com.jetbrains.interactiveRebase.toolWindow

import com.jetbrains.interactiveRebase.components.BranchPanel
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.content.ContentFactory
import com.jetbrains.interactiveRebase.services.MyProjectService

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
//                val c = CirclePanel()
//                c.preferredSize = Dimension(20, 20)

                // add(node)
                add(
                    BranchPanel(
                        listOf(
                            "Initial commit",
                            "Added feature X",
                            "Fixed issue #123",
                            "Refactored code",
                            "Merged branch 'feature-x' into 'main'"
                        )
                    )
                )
//                add(c, BorderLayout.CENTER)
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
