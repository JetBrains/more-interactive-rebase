package com.jetbrains.interactiveRebase.providers

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.interactiveRebase.editors.IRFileEditorBase

/**
 * A FileEditorProvider for the IRVirtualFile. It is used to
 * create the Editor Tab for the Interactive Rebase feature.
 */
class IRFileEditorProvider : FileEditorProvider, DumbAware {
    /**
     * Checks whether a file should be opened in an editor provided by this provider.
     *
     * @param file file to be tested for acceptance.
     * @return `true` if provider can create valid editor for the specified `file`.
     */
    override fun accept(
        project: Project,
        file: VirtualFile,
    ): Boolean {
        return file.path == "ir:/Interactive Rebase"
    }

    /**
     * Creates editor for the specified file.
     *
     * This method is called only if the provider has accepted this file (i.e., method [.accept] returned
     * `true`).
     *
     * @return created editor for specified file.
     */
    override fun createEditor(
        project: Project,
        file: VirtualFile,
    ): FileEditor {
        return IRFileEditorBase(project, file)
    }

    /**
     * @return the ID of the editor type provided by this provider.
     */
    override fun getEditorTypeId(): String {
        return "IRFileEditorProvider"
    }

    /**
     * @return the policy for opening files of this type,
     * which hides the default editor for the IRVirtualFile.
     */
    override fun getPolicy(): FileEditorPolicy {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR
    }
}
