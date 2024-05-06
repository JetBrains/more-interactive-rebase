package com.jetbrains.interactiveRebase.service

import com.intellij.openapi.components.service
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.services.IRVirtualFileService

class IRVirtualFileServiceTest : BasePlatformTestCase() {
    fun testGetVirtualFileForProject() {
        val projectService = project.service<IRVirtualFileService>()
        val virtualFile = projectService.getVirtualFileForProject()
        assertEquals("Interactive Rebase", virtualFile.name)
    }

    /**
     * Tests the creation and opening of the IRVirtualFile.
     */
    fun testCreateAndOpenIRVirtualFile() {
        val projectService = project.service<IRVirtualFileService>()

        // this succeeds only if the provider for the virtual file is the one
        // we created for the plugin
        projectService.createAndOpenIRVirtualFile()

        // this tests if the file was actually opened by the provider
        val psiManager = PsiManager.getInstance(project)
        val psiFile: PsiFile? = psiManager.findFile(projectService.getVirtualFileForProject())
        assertEquals("Interactive Rebase", psiFile?.name)
    }
}
