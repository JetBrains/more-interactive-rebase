package com.jetbrains.interactiveRebase.provider

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.providers.IRFileIconProvider
import com.jetbrains.interactiveRebase.services.IRVirtualFileService
import com.jetbrains.interactiveRebase.virtualFile.IRVirtualFileSystem
import org.assertj.core.api.Assertions.assertThat

class IRFileIconProviderTest : BasePlatformTestCase() {
    private lateinit var provider: IRFileIconProvider

    override fun setUp() {
        super.setUp()
        provider = IRFileIconProvider()

    }

    fun testGetIconVirtualFile(){
        val virtualFileSystem = IRVirtualFileSystem()
        val virtualFile = virtualFileSystem.findFileByPath("Interactive Rebase")
        assertThat(provider.getIcon(virtualFile, 0,project)).isEqualTo(AllIcons.Vcs.Branch)
    }

    fun testGetIconNotVirtual(){
        val file = LightVirtualFile("jef")
        assertThat(provider.getIcon(file, 0,project)).isEqualTo(file.fileType.icon)
    }
}

