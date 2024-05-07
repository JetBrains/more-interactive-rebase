package com.jetbrains.interactiveRebase.virtualFile

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import java.io.InputStream
import java.io.OutputStream

/**
 * A virtual file that is used for the <i>Interactive Rebase</i>
 * functionality of the plugin.
 *
 * It will always have a singular instance for an opened project
 * in the IDE. It is saved in the custom IRVirtualFileSystem.
 */
class IRVirtualFile : VirtualFile() {
    /**
     * Gets the name of this file.
     * @return "Interactive Rebase"
     */
    override fun getName(): String {
        return "Interactive Rebase"
    }

    /**
     * Gets the VirtualFileSystem this file belongs to.
     * @return an instance of IRVirtualFileSystem
     */
    override fun getFileSystem(): VirtualFileSystem {
        return IRVirtualFileSystem()
    }

    /**
     * Gets the path of this file.
     * @return the path
     */
    override fun getPath(): String {
        return "ir:/$name"
    }

    /**
     * Checks whether this file could be modified.
     *
     * @return `false`, since the file in itself is always empty, has no content.
     */
    override fun isWritable(): Boolean {
        return false
    }

    /**
     * Checks whether this file is a directory.
     *
     * @return `false`, since there is only one file in its file system.
     */
    override fun isDirectory(): Boolean {
        return false
    }

    /**
     * Checks whether this `VirtualFile` is valid. (Dummy method)
     * @return `true`
     */
    override fun isValid(): Boolean {
        return true
    }

    /**
     * Gets the parent `VirtualFile`.
     * @return null, since it has no parent in its file system
     */
    override fun getParent(): VirtualFile? {
        return null
    }

    /**
     * Gets the child files.
     *
     * @return `null` because the file is not a directory.
     */
    override fun getChildren(): Array<VirtualFile>? {
        return null
    }

    /**
     * File length in bytes.
     *
     * @return 0, since the file is empty.
     */
    override fun getLength(): Long {
        return 0L
    }

    /**
     * This operation is not supported by this implementation. Calling this method
     * will always result in {@link UnsupportedOperationException}.
     */
    override fun getOutputStream(
        requestor: Any?,
        newModificationStamp: Long,
        newTimeStamp: Long,
    ): OutputStream {
        throw UnsupportedOperationException("getOutputStream is not supported for the IRVirtualFile")
    }

    /**
     * This operation is not supported by this implementation. Calling this method
     * will always result in {@link UnsupportedOperationException}.
     */
    override fun contentsToByteArray(): ByteArray {
        throw UnsupportedOperationException("contentsToByteArray is not supported for the IRVirtualFile")
    }

    /**
     * This operation is not supported by this implementation. Calling this method
     * will always result in {@link UnsupportedOperationException}.
     */
    override fun getTimeStamp(): Long {
        throw UnsupportedOperationException("getTimeStamp is not supported for the IRVirtualFile")
    }

    /**
     * This operation is not supported by this implementation. Calling this method
     * will always result in {@link UnsupportedOperationException}.
     */
    override fun refresh(
        asynchronous: Boolean,
        recursive: Boolean,
        postRunnable: Runnable?,
    ) {
        throw UnsupportedOperationException("refresh is not supported for the IRVirtualFile")
    }

    /**
     * This operation is not supported by this implementation. Calling this method
     * will always result in {@link UnsupportedOperationException}.
     */
    override fun getInputStream(): InputStream {
        throw UnsupportedOperationException("getInputStream is not supported for the IRVirtualFile")
    }
}
