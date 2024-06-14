package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.interactiveRebase.providers.IRFileEditorProvider
import com.jetbrains.interactiveRebase.virtualFile.IRVirtualFileSystem

/**
 * Service for the logic of dealing with IRVirtualFiles
 * (Virtual Files created for the Interactive Rebase feature)
 */
@Service(Service.Level.PROJECT)
class IRVirtualFileService(private val project: Project) {
    private val virtualFileSystem = IRVirtualFileSystem()

    /**
     * Gets the IRVirtualFile associated with the project.
     *
     * @return the IRVirtualFile
     */
    fun getVirtualFileForProject(): VirtualFile {
        return virtualFileSystem.findFileByPath("Interactive Rebase")
    }

    /**
     * Creates and opens the IRVirtualFile associated with the project.
     */
    fun createAndOpenIRVirtualFile() {
        val virtualFile = getVirtualFileForProject()
        virtualFile.putUserData(FileEditorProvider.KEY, IRFileEditorProvider())
        FileEditorManager.getInstance(project).openFile(virtualFile, true)
    }

    /**
     * Closes the IRVirtualFile associated with the project.
     */
    fun closeIRVirtualFile() {
        val virtualFile = getVirtualFileForProject()
        FileEditorManager.getInstance(project).closeFile(virtualFile)
    }
}
