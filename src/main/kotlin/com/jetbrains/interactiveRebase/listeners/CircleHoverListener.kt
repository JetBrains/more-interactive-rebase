package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.components.service
import com.jetbrains.interactiveRebase.actions.gitPanel.RebaseActionsGroup
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.visuals.BranchPanel
import com.jetbrains.interactiveRebase.visuals.CirclePanel
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

/**
 * A listener that allows a circle panel to be hovered on.
 * Involves the implementation of three methods for different type of
 * mouse actions that all reflect different parts of a "hover" action
 */

class CircleHoverListener(val circlePanel: CirclePanel) : MouseAdapter(), Disposable {
    internal val commit: CommitInfo = circlePanel.commit
    val branchInfo = (circlePanel.parent as BranchPanel).branch

    /**
     * Highlight the circle if the mouse enters the encapsulating rectangle and
     * is within the drawn circle.
     */
    private val actionManager = ActionManager.getInstance()
    private val actionsGroup =
        actionManager.getAction(
            "com.jetbrains.interactiveRebase.actions.gitPanel.RebaseActionsGroup",
        ) as RebaseActionsGroup

    override fun mouseEntered(e: MouseEvent?) {
        if (e != null && circlePanel.circle.contains(e.x.toDouble(), e.y.toDouble())) {
            circlePanel.commit.isHovered = true
            circlePanel.repaint()
            e.consume()
        }
    }

    /**
     * Remove hovering if the mouse exits the encapsulating rectangle.
     */

    override fun mouseExited(e: MouseEvent?) {
        if (e != null && !circlePanel.circle.contains(e.x.toDouble(), e.y.toDouble())) {
            circlePanel.commit.isHovered = false
            circlePanel.repaint()
            e.consume()
        }
    }

    /**
     * Select a commit upon a click.
     */
    override fun mouseClicked(e: MouseEvent?) {
        if (e != null) {
            if (e.isShiftDown) {
                shiftClick()
                return
            } else if (e.isMetaDown || e.isControlDown) {
                controlClick()
                return
            }
            if (circlePanel.commit.isCollapsed) {
                commit.project.service<ActionService>().expandCollapsedCommits(commit, branchInfo)
                commit.isHovered = false
            } else if (e.button == MouseEvent.BUTTON1) {
                if (e.clickCount >= 2 &&
                    !commit.getChangesAfterPick().any { change -> change is DropCommand } &&
                    branchInfo.isWritable
                ) {
                    commit.setTextFieldEnabledTo(true)
                    commit.project.service<ModelService>().selectSingleCommit(commit, branchInfo)
                } else {
                    normalClick()
                }
            }

            e.consume()
        }
    }

    /**
     * Open context menu
     */
    override fun mousePressed(e: MouseEvent?) {
        if (e != null && e.isPopupTrigger) {
            invokePopup(e.x, e.y)
            e.consume()
        }
    }

    /**
     * Open context menu
     */
    override fun mouseReleased(e: MouseEvent?) {
        if (e != null && e.isPopupTrigger) {
            invokePopup(e.x, e.y)
            e.consume()
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
        e?.consume()
    }

    /**
     * Selects a single commit if not selected and
     * deselects if selected
     */

    private fun normalClick() {
        val modelService = commit.project.service<ModelService>()
        if (!circlePanel.commit.isSelected || branchInfo.getActualSelectedCommitsSize() > 1) {
            modelService.selectSingleCommit(circlePanel.commit, branchInfo)
        } else {
            modelService.removeFromSelectedCommits(circlePanel.commit, branchInfo)
        }
    }

    /**
     * Selects the range of commits
     * between the clicked and last
     * selected commit
     */
    private fun shiftClick() {
        val modelService = commit.project.service<ModelService>()

        if (branchInfo != modelService.getSelectedBranch()) {
            modelService.clearSelectedCommits()
        }

        val selectedCommits = modelService.getSelectedCommits()
        if (selectedCommits.isEmpty()) {
            normalClick()
            return
        }
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
        val modelService = commit.project.service<ModelService>()

        if (branchInfo != modelService.getSelectedBranch()) {
            modelService.clearSelectedCommits()
        }

        val selectedCommits = modelService.getSelectedCommits()
        if (selectedCommits.isEmpty()) {
            normalClick()
            return
        }

        if (!commit.isSelected) {
            modelService.addToSelectedCommits(commit, branchInfo)
        } else {
            modelService.removeFromSelectedCommits(commit, branchInfo)
        }
    }

    /**
     * Shows context menu
     */

    fun invokePopup(
        x: Int,
        y: Int,
    ) {
        val modelService = commit.project.service<ModelService>()
        val mainPanel = commit.project.service<ActionService>().mainPanel

        val point = SwingUtilities.convertPoint(circlePanel, x, y, mainPanel)

        if (!commit.isSelected) {
            modelService.selectSingleCommit(circlePanel.commit, branchInfo)
        }

        mainPanel.invokePopup(point.x, point.y)
    }

    override fun dispose() {
    }
}
