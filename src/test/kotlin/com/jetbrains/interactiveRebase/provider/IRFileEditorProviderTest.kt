package com.jetbrains.interactiveRebase.provider

import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.editors.IRFileEditorBase
import com.jetbrains.interactiveRebase.providers.IRFileEditorProvider
import com.jetbrains.interactiveRebase.virtualFile.IRVirtualFileSystem
import junit.framework.TestCase
import org.assertj.core.api.Assertions.assertThat

class IRFileEditorProviderTest : BasePlatformTestCase() {
    fun testAccept() {
        val provider = IRFileEditorProvider()
        val virtualFileSystem = IRVirtualFileSystem()
        val virtualFile = virtualFileSystem.findFileByPath("Interactive Rebase")
        assertTrue(provider.accept(project, virtualFile))
    }

    fun testDontAccept() {
        val provider = IRFileEditorProvider()
        assertFalse(provider.accept(project, LightVirtualFile("jef")))
    }

    fun testCreateEditor() {
        val provider = IRFileEditorProvider()
        val virtualFileSystem = IRVirtualFileSystem()
        val virtualFile = virtualFileSystem.findFileByPath("Interactive Rebase")
        TestCase.assertNotNull(provider.createEditor(project, virtualFile))
        assertThat(provider.createEditor(project, virtualFile)).isInstanceOf(IRFileEditorBase::class.java)
    }

    fun testGetEditorTypeId() {
        val provider = IRFileEditorProvider()
        assertEquals(provider.getEditorTypeId(), "IRFileEditorProvider")
    }

    fun testGetPolicy() {
        val provider = IRFileEditorProvider()
        assertEquals(provider.getPolicy(), FileEditorPolicy.HIDE_DEFAULT_EDITOR)
    }
}
