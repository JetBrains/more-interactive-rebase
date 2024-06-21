package com.jetbrains.interactiveRebase.providers

import com.intellij.icons.AllIcons
import com.intellij.ide.FileIconProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.interactiveRebase.virtualFile.IRVirtualFile
import javax.swing.Icon

class IRFileIconProvider : FileIconProvider {
    override fun getIcon(
        file: VirtualFile,
        flags: Int,
        project: Project?,
    ): Icon {
        if (file is IRVirtualFile) {
            return AllIcons.Vcs.Branch
        }
        return file.fileType.icon
    }
}
