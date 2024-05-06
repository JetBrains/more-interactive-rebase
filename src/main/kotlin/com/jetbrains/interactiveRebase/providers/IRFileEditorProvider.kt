package com.jetbrains.interactiveRebase.providers

import com.intellij.diff.util.FileEditorBase
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.visuals.Branch
import com.jetbrains.interactiveRebase.visuals.LabeledBranchPanel
import com.jetbrains.interactiveRebase.visuals.Palette
import java.awt.Color
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

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
        return MyFileEditorBase(project, file)
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

    /**
     * A FileEditorBase for the IRVirtualFile.
     * It is used to create the Editor Tab for the Interactive Rebase feature.
     */
    class MyFileEditorBase(private val project: Project, private val virtualFile: VirtualFile) : FileEditorBase() {
        /**
         * Returns a component which represents the editor in UI.
         *
         * @return the Swing component for the editor UI
         */
        override fun getComponent(): JComponent {
            val panel = JBPanel<JBPanel<*>>()
            panel.layout = GridBagLayout()
            val gbc = GridBagConstraints()
            gbc.gridx = 0
            gbc.gridy = 0
            gbc.fill = GridBagConstraints.BOTH
            panel.add(
                LabeledBranchPanel(
                    Branch(
                        true,
                        "MAIN",
                        listOf(
                            "Initial commit",
                            "Added feature X",
                            "Fixed issue #123",
                            "Refactored code",
                            "Merged branch 'feature-x' into 'main'",
                        ),
                    ),
                    Palette.BLUE,
                    SwingConstants.RIGHT,
                ),
                gbc,
            )

            gbc.gridx = 1
            gbc.gridy = 0
            gbc.fill = GridBagConstraints.BOTH
            panel.add(
                LabeledBranchPanel(
                    Branch(
                        false,
                        "dev",
                        listOf(
                            "Initial commit",
                            "Added feature X",
                            "Fixed issue #123",
                            "Refactored code",
                            "Merged branch 'feature-x' into 'main'",
                        ),
                    ),
                    Palette.TOMATO,
                ),
                gbc,
            )
            return panel
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
            // TODO: Implement the editor UI
            val panel = JPanel()
            panel.background = Color.LIGHT_GRAY
            return panel
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
}
