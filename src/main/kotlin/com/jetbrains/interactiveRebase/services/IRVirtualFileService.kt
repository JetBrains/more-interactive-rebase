package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileSystem
import com.jetbrains.interactiveRebase.virtualFile.IRVirtualFileSystem

/**
 * Service for the logic of dealing with IRVirtualFiles
 * (Virtual Files created for the Interactive Rebase feature)
 */
@Service(Service.Level.PROJECT)
class IRVirtualFileService(private val project: Project) {
    private val virtualFileSystem: VirtualFileSystem = IRVirtualFileSystem()

    /**
     * Creates and opens the IRVirtualFile associated with the project.
     */
    fun createAndOpenIRVirtualFile() {
        val virtualFile = virtualFileSystem.findFileByPath("Interactive Rebase")
        if (virtualFile != null) {
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
        }
    }
}
