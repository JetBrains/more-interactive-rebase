package com.jetbrains.interactiveRebase.virtualFile

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileSystem

/**
 * A VirtualFileSystem for the IRVirtualFile
 * (The file that is opened when the Interactive Rebase feature is invoked).
 *
 * It extends the VirtualFileSystem class from the IntelliJ Platform SDK,
 * but only implements the methods that are necessary for the IRVirtualFile,
 * the rest of the methods being "unsupported" methods, as they are never called.
 *
 * This fileSystem only holds a singular instance of the IRVirtualFile, and it
 * is in itself of a singular instance for each project opened in the IDE.
 */
class IRVirtualFileSystem : VirtualFileSystem() {
    private val file = IRVirtualFile()

    /**
     * Gets the protocol of this file system.
     *
     * @return the protocol
     */
    override fun getProtocol(): String {
        return PROTOCOL
    }

    /**
     * Finds a file by its path.
     * This file system has a singular file, which is always the same,
     * and this method returns it.
     *
     * @param path the path of the file
     * @return the file
     */
    override fun findFileByPath(path: String): VirtualFile {
        return file
    }

    /**
     * The IRVirtualFile is always empty and thus
     * always read-only.
     *
     * @return true always
     */
    override fun isReadOnly(): Boolean {
        return true
    }

    /**
     * This operation is not supported by this implementation. Calling this method
     * will always result in {@link UnsupportedOperationException}.
     */
    override fun refresh(asynchronous: Boolean) {
        throw UnsupportedOperationException("refresh is not supported for the IRVirtualFileSystem")
    }

    /**
     * This operation is not supported by this implementation. Calling this method
     * will always result in {@link UnsupportedOperationException}.
     */
    override fun refreshAndFindFileByPath(path: String): VirtualFile? {
        throw UnsupportedOperationException("refreshAndFindFileByPath is not supported for the IRVirtualFileSystem")
    }

    /**
     * This operation is not supported by this implementation. Calling this method
     * will always result in {@link UnsupportedOperationException}.
     */
    override fun addVirtualFileListener(listener: VirtualFileListener) {
        throw UnsupportedOperationException("addVirtualFileListener is not supported for the IRVirtualFileSystem")
    }

    /**
     * This operation is not supported by this implementation. Calling this method
     * will always result in {@link UnsupportedOperationException}.
     */
    override fun removeVirtualFileListener(listener: VirtualFileListener) {
        throw UnsupportedOperationException("removeVirtualFileListener is not supported for the IRVirtualFileSystem")
    }

    /**
     * This operation is not supported by this implementation. Calling this method
     * will always result in {@link UnsupportedOperationException}.
     */
    override fun deleteFile(
        requestor: Any?,
        vFile: VirtualFile,
    ) {
        throw UnsupportedOperationException("deleteFile is not supported for the IRVirtualFileSystem")
    }

    /**
     * This operation is not supported by this implementation. Calling this method
     * will always result in {@link UnsupportedOperationException}.
     */
    override fun moveFile(
        requestor: Any?,
        vFile: VirtualFile,
        newParent: VirtualFile,
    ) {
        throw UnsupportedOperationException("moveFile is not supported for the IRVirtualFileSystem")
    }

    /**
     * This operation is not supported by this implementation. Calling this method
     * will always result in {@link UnsupportedOperationException}.
     */
    override fun renameFile(
        requestor: Any?,
        vFile: VirtualFile,
        newName: String,
    ) {
        throw UnsupportedOperationException("renameFile is not supported for the IRVirtualFileSystem")
    }

    /**
     * This operation is not supported by this implementation. Calling this method
     * will always result in {@link UnsupportedOperationException}.
     */
    override fun createChildFile(
        requestor: Any?,
        vDir: VirtualFile,
        fileName: String,
    ): VirtualFile {
        throw UnsupportedOperationException("createChildFile is not supported for the IRVirtualFileSystem")
    }

    /**
     * This operation is not supported by this implementation. Calling this method
     * will always result in {@link UnsupportedOperationException}.
     */
    override fun createChildDirectory(
        requestor: Any?,
        vDir: VirtualFile,
        dirName: String,
    ): VirtualFile {
        throw UnsupportedOperationException("createChildDirectory is not supported for the IRVirtualFileSystem")
    }

    /**
     * This operation is not supported by this implementation. Calling this method
     * will always result in {@link UnsupportedOperationException}.
     */
    override fun copyFile(
        requestor: Any?,
        virtualFile: VirtualFile,
        newParent: VirtualFile,
        copyName: String,
    ): VirtualFile {
        throw UnsupportedOperationException("copyFile is not supported for the IRVirtualFileSystem")
    }

    companion object {
        private const val PROTOCOL = "ir"
    }
}
