package com.jetbrains.interactiveRebase.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.jetbrains.interactiveRebase.services.IRVirtualFileService

class CreateEditorTabAction : AnAction(
//    CustomIcon.IRIcon
) {
    /**
     * This method is called when the entry point
     * for the plugin is invoked.
     *
     * It creates the IRVirtualFile, which is a unique file
     * for each opened project. It then opens the file in the editor.
     *
     * @param e Carries information on the invocation place
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        project?.service<IRVirtualFileService>()?.createAndOpenIRVirtualFile()
    }
}
