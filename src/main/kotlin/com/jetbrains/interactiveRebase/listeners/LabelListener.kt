package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.services.ModelService
import java.awt.event.MouseEvent
import java.awt.event.MouseListener

class LabelListener(private val commitInfo: CommitInfo) : MouseListener, Disposable {
    private val project = commitInfo.project
    private val modelService = project.service<ModelService>()

    /**
     * If double-clicked, enables the right flags to make text field visible
     * If clicked once, selects or deselects commit
     */
    override fun mouseClicked(e: MouseEvent?) {
        if (e != null && e.clickCount >= 2 && !commitInfo.changes.any { change -> change is DropCommand }) {
            commitInfo.setTextFieldEnabledTo(true)
            commitInfo.isSelected = true
            modelService.addOrRemoveCommitSelection(commitInfo)
        }
        if (e != null && e.clickCount == 1) {
            commitInfo.isSelected = !commitInfo.isSelected
            modelService.addOrRemoveCommitSelection(commitInfo)
        }
    }

    override fun mousePressed(e: MouseEvent?) {}

    override fun mouseReleased(e: MouseEvent?) {}

    /**
     * When the mouse hovers over the label, the related circle is set to hovered
     */
    override fun mouseEntered(e: MouseEvent?) {}

    /**
     * de-hovers the circle if the mouse exits
     */
    override fun mouseExited(e: MouseEvent?) {}

    override fun dispose() {
    }
}
