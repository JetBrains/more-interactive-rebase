package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.utils.gitUtils.IRGitUtils
import java.awt.AWTEvent
import java.awt.event.AWTEventListener
import java.awt.event.WindowEvent
import javax.swing.JDialog

class PopupListener(val project: Project) : AWTEventListener, Disposable {
    var gitUtils = IRGitUtils(project)
    val log = Logger.getInstance(PopupListener::class.java)

    /**
     * Secondary constructor for testing
     */
    constructor(project: Project, gitUtils: IRGitUtils) : this(project) {
        this.gitUtils = gitUtils
    }

    /**
     * Hides the conflicts dialog such that the editor tab
     * refreshes and then shows the dialog again
     */
    override fun eventDispatched(event: AWTEvent) {
        if (event.id == WindowEvent.WINDOW_OPENED) {
            try {
                val source = event.source as JDialog

                if (source.title == "Conflicts") {
                    val root = gitUtils.getRoot()
                    if (root == null) {
                        log.info("Repository root is null")
                        return
                    }
                    val modelService = project.service<ModelService>()
                    val currentMergingCommit = gitUtils.getCurrentRebaseCommit(project, root)

                    if (currentMergingCommit != modelService.previousConflictCommit) {
                        source.dispose()
                        modelService.showCustomMergeDialog(currentMergingCommit, root)
                    }
                }
            } catch (e: ClassCastException) {
                print("")
            }
        }
    }

    override fun dispose() {
    }
}
