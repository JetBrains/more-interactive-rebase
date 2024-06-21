package com.jetbrains.interactiveRebase.actions

import com.intellij.openapi.components.service
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.services.IRVirtualFileService

class CreateEditorTabActionTest : BasePlatformTestCase() {
    private lateinit var action: CreateEditorTabAction

    override fun setUp() {
        super.setUp()
        action = CreateEditorTabAction()
    }

    fun testActionPerformed() {
        val projectService = project.service<IRVirtualFileService>()

        // this succeeds only if the provider for the virtual file is the one
        // we created for the plugin
        val testEvent = TestActionEvent.createTestEvent()
        action.actionPerformed(testEvent)

        // this tests if the file was actually opened by the provider
        val psiManager = PsiManager.getInstance(project)
        val psiFile: PsiFile? = psiManager.findFile(projectService.getVirtualFileForProject())
        assertEquals("Interactive Rebase", psiFile?.name)
    }
}
