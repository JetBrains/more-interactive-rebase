package com.jetbrains.interactiveRebase.providers

import com.intellij.ide.FileIconProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.interactiveRebase.virtualFile.IRVirtualFile
import CustomIcon
import com.intellij.icons.AllIcons
import javax.swing.Icon

class IRFileIconProvider : FileIconProvider {
    override fun getIcon(
        file: VirtualFile,
        flags: Int,
        project: Project?,
    ): Icon {
        if (file is IRVirtualFile) {
            return AllIcons.Actions.Lightning
//            return ImageIcon(
//                this::class.java.getResource(
//                    "/smallIcon.svg"
//                )
//            )
//            makeIcon()
        }
        return file.fileType.icon
    }

//    fun makeIcon(): Icon {
////        return IconManager.getInstance()
////            .loadRasterizedIcon(
////                "src/main/resources/smallIcon.svg",
////                DvcsImplIcons::class.java.classLoader,
////                7,
////                0
////            )
//        return CustomIcon.IRIcon
//    }
}
