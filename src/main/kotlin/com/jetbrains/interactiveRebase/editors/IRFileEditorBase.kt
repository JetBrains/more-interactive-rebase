package com.jetbrains.interactiveRebase.editors

import com.intellij.diff.util.FileEditorBase
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.visuals.MainPanel
import javax.swing.JComponent

/**
 * A FileEditorBase for the IRVirtualFile.
 * It is used to create the Editor Tab for the Interactive Rebase feature.
 */
class IRFileEditorBase(private val project: Project, private val virtualFile: VirtualFile) : FileEditorBase() {
    private val modelService = project.service<ModelService>()
    private var component = MainPanel(project, modelService.branchInfo)

    /**
     * Returns a component which represents the editor in UI.
     *
     * @return the Swing component for the editor UI
     */
    override fun getComponent(): JComponent {
        return component
    }

    /**
     * Returns editor's name to be shown in UI.
     */
    override fun getName(): String {
        return "Interactive Rebase Editor Tab"
    }

    /**
     * Returns a component to be focused when the editor is opened.
     *
     * @return the Swing component to be focused
     */
    override fun getPreferredFocusedComponent(): JComponent {
        return component
    }

    /**
     * Returns the file associated with the editor.
     *
     * @return the file associated with the editor
     */
    override fun getFile(): VirtualFile {
        return virtualFile
    }
}
