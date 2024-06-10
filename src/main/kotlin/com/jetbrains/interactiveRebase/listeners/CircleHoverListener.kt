package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.visuals.BranchPanel
import com.jetbrains.interactiveRebase.visuals.CirclePanel
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

/**
 * A listener that allows a circle panel to be hovered on.
 * Involves the implementation of three methods for different type of
 * mouse actions that all reflect different parts of a "hover" action
 */
class CircleHoverListener(private val circlePanel: CirclePanel) : MouseAdapter(), Disposable {
    val commit: CommitInfo = circlePanel.commit
    val branchInfo = (circlePanel.parent as BranchPanel).branch

    /**
     * Highlight the circle if the mouse enters the encapsulating rectangle and
     * is within the drawn circle.
     */

    override fun mouseEntered(e: MouseEvent?) {
        if (e != null && circlePanel.circle.contains(e.x.toDouble(), e.y.toDouble())) {
            circlePanel.commit.isHovered = true
            circlePanel.repaint()
        }
    }

    /**
     * Remove hovering if the mouse exits the encapsulating rectangle.
     */

    override fun mouseExited(e: MouseEvent?) {
        if (e != null && !circlePanel.circle.contains(e.x.toDouble(), e.y.toDouble())) {
            circlePanel.commit.isHovered = false
            circlePanel.repaint()
        }
    }

    /**
     * Select a commit upon a click.
     */
    override fun mouseClicked(e: MouseEvent?) {
        val modelService = commit.project.service<ModelService>()
        if (e?.isShiftDown!!) {
            shiftClick()
            return
        } else if (e.isMetaDown || e.isControlDown) {
            controlClick()
            return
        }

        if (!circlePanel.commit.isSelected || modelService.branchInfo.getActualSelectedCommitsSize() > 1) {
            modelService.selectSingleCommit(circlePanel.commit, branchInfo)
        } else {
            modelService.removeFromSelectedCommits(circlePanel.commit, branchInfo)
        }
    }

    /**
     * Highlights a circle if the mouse has entered the encapsulating rectangle panel
     * and has subsequently moved within the panel,
     * so that it is now within the circle boundaries.
     */

    override fun mouseMoved(e: MouseEvent?) {
        circlePanel.commit.isHovered = e != null && circlePanel.circle.contains(e.x.toDouble(), e.y.toDouble())
        circlePanel.repaint()
    }

    /**
     * Selects the range of commits
     * between the clicked and last
     * selected commit
     */
    private fun shiftClick() {
        val modelService = commit.project.service<ModelService>()
        val selectedCommits = modelService.getSelectedCommits()
        val selectedCommit = selectedCommits[0]
        val selectedIndex = modelService.getCurrentCommits().indexOf(selectedCommit)
        val commitIndex = modelService.getCurrentCommits().indexOf(commit)

        modelService.selectSingleCommit(selectedCommit, branchInfo)

        modelService.getCurrentCommits()
            .subList(Integer.min(selectedIndex, commitIndex), Integer.max(selectedIndex + 1, commitIndex + 1))
            .forEach {
                if (it != selectedCommit) {
                    modelService.addToSelectedCommits(it, branchInfo)
                }
            }
    }

    /**
     * Adds to the selected commits
     * the currently selected commit
     */
    private fun controlClick() {
        val branchInfo = (circlePanel.parent as BranchPanel).branch

        val modelService = commit.project.service<ModelService>()
        if (!commit.isSelected) {
            modelService.addToSelectedCommits(commit, branchInfo)
        } else {
            modelService.removeFromSelectedCommits(commit, branchInfo)
        }
    }

    override fun dispose() {
    }
}
