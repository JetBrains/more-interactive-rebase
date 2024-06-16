package com.jetbrains.interactiveRebase.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages

/**
 * Class used for dependency injection of user input dialogs
 */
@Service(Service.Level.PROJECT)
class DialogService(private val project: Project) {
    /**
     * Triggers warning dialog with the given title and description
     */
    fun warningYesNoDialog(
        title: String,
        description: String,
    ): Boolean {
        return MessageDialogBuilder
            .yesNo(
                title,
                description,
                Messages.getWarningIcon(),
            )
            .ask(project)
    }

    /**
     * Triggers warning dialog with the given title and description
     */
    fun warningOkCancelDialog(
        title: String,
        description: String,
    ): Boolean {
        return MessageDialogBuilder
            .okCancel(
                title,
                description,
            )
            .ask(project)
    }
}
