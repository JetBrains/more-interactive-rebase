package com.jetbrains.interactiveRebase.virtualFile

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert
import org.mockito.Mockito.mock

class IRVirtualFileSystemTest : BasePlatformTestCase() {
    fun testGetProtocol() {
        val virtualFileSystem = IRVirtualFileSystem()
        Assert.assertEquals("ir", virtualFileSystem.protocol)
    }

    fun testFindFileByPath() {
        val virtualFileSystem = IRVirtualFileSystem()
        val file = virtualFileSystem.findFileByPath("Interactive Rebase")
        Assert.assertNotNull(file)
        Assert.assertEquals("Interactive Rebase", file.name)
        Assert.assertEquals("ir:/Interactive Rebase", file.path)
    }

    fun testIsReadOnly() {
        val virtualFileSystem = IRVirtualFileSystem()
        Assert.assertTrue(virtualFileSystem.isReadOnly)
    }

    fun testUnsupportedOperations() {
        val virtualFileSystem = IRVirtualFileSystem()

        listOf(
            { virtualFileSystem.refresh(false) },
            { virtualFileSystem.refreshAndFindFileByPath("somePath") },
            { virtualFileSystem.addVirtualFileListener(object : VirtualFileListener {}) },
            { virtualFileSystem.removeVirtualFileListener(object : VirtualFileListener {}) },
            { virtualFileSystem.deleteFile(this, virtualFileSystem.findFileByPath("Interactive Rebase")) },
            { virtualFileSystem.moveFile(this, mock(VirtualFile::class.java), mock(VirtualFile::class.java)) },
            { virtualFileSystem.renameFile(this, mock(VirtualFile::class.java), "name") },
            { virtualFileSystem.createChildFile(this, mock(VirtualFile::class.java), "name") },
            { virtualFileSystem.createChildDirectory(this, mock(VirtualFile::class.java), "name") },
            { virtualFileSystem.copyFile(this, mock(VirtualFile::class.java), mock(VirtualFile::class.java), "name") },
        ).forEach { testOperation ->
            try {
                testOperation.invoke()
                Assert.fail("Expected UnsupportedOperationException was not thrown")
            } catch (e: UnsupportedOperationException) {
                // The expected behavior of these dummy methods is to do nothing other than throw an exception.
            }
        }
    }
}
