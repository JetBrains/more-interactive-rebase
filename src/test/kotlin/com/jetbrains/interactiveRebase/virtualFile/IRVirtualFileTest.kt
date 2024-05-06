package com.jetbrains.interactiveRebase.virtualFile

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert

class IRVirtualFileTest : BasePlatformTestCase() {
    fun testGetName() {
        val virtualFile = IRVirtualFile()
        assertEquals("Interactive Rebase", virtualFile.name)
    }

    fun testGetFileSystem() {
        val virtualFile = IRVirtualFile()
        assertEquals(IRVirtualFileSystem::class.java, virtualFile.fileSystem::class.java)
    }

    fun testGetPath() {
        val virtualFile = IRVirtualFile()
        assertEquals("ir:/Interactive Rebase", virtualFile.path)
    }

    fun testIsWritable() {
        val virtualFile = IRVirtualFile()
        assertFalse(virtualFile.isWritable)
    }

    fun testIsDirectory() {
        val virtualFile = IRVirtualFile()
        assertFalse(virtualFile.isDirectory)
    }

    fun testIsValid() {
        val virtualFile = IRVirtualFile()
        assertTrue(virtualFile.isValid)
    }

    fun testGetLength() {
        val virtualFile = IRVirtualFile()
        assertEquals(0, virtualFile.length)
    }

    fun testGetChildren() {
        val virtualFile = IRVirtualFile()
        virtualFile.children?.let { assertEmpty(it) }
    }

    fun testGetParent() {
        val virtualFile = IRVirtualFile()
        assertNull(virtualFile.parent)
    }

    fun testUnsupportedOperations() {
        val virtualFile = IRVirtualFile()

        // Test each unsupported operation
        listOf(
            { virtualFile.getOutputStream(this, 0L, 0L) },
            { virtualFile.contentsToByteArray() },
            { virtualFile.timeStamp },
            { virtualFile.refresh(false, false, null) },
            { virtualFile.inputStream },
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
