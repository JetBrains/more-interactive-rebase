package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.components.service
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.services.ModelService
import java.awt.event.MouseEvent
import java.awt.event.MouseListener

class LabelListener(private val commitInfo: CommitInfo) : MouseListener {
    private val project = commitInfo.project
    private val modelService = project.service<ModelService>()

    /**
     * If double-clicked, enables the right flags to make text field visible
     * If clicked once, selects or deselects commit
     */
    override fun mouseClicked(e: MouseEvent?) {
        if (e != null && e.clickCount >= 2) {
            commitInfo.setDoubleClickedTo(true)
            selectCommitIfNotSelected()
        }
        if (e != null && e.clickCount == 1) {
            selectOrDeselectCommit()
        }
    }

    /**
     * Selects a commit if it is not selected, deselects a commit if it is selected
     */
    private fun selectOrDeselectCommit() {
        if (selectCommitIfNotSelected()) {
            modelService.branchInfo.removeSelectedCommits(commitInfo)
            commitInfo.setSelectedTo(false)
        }
    }

    /**
     * Selects a commit if it is not already selected, returns false if commit was already selected
     * and true if it was not selected initially
     */
    private fun selectCommitIfNotSelected(): Boolean {
        if (!commitInfo.isSelected && !modelService.branchInfo.selectedCommits.contains(commitInfo)) {
            modelService.branchInfo.addSelectedCommits(commitInfo)
            commitInfo.setSelectedTo(true)
            return false
        }
        return true
    }

    override fun mousePressed(e: MouseEvent?) {}

    override fun mouseReleased(e: MouseEvent?) {}

    /**
     * When the mouse hovers over the label, the related circle is set to hovered
     */
    override fun mouseEntered(e: MouseEvent?) {
        commitInfo.setHoveredTo(true)
    }

    /**
     * de-hovers the circle if the mouse exits
     */
    override fun mouseExited(e: MouseEvent?) {
        commitInfo.setHoveredTo(false)
    }
}
