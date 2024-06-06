package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.components.service
import com.intellij.ui.PopupHandler
import com.jetbrains.interactiveRebase.actions.gitPanel.RebaseActionsGroup
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.visuals.CirclePanel
import java.awt.Component
import java.awt.event.MouseEvent

/**
 * A listener that allows a circle panel to be hovered on.
 * Involves the implementation of three methods for different type of
 * mouse actions that all reflect different parts of a "hover" action
 */

class CircleHoverListener(private val circlePanel: CirclePanel) : PopupHandler(), Disposable {
    val commit: CommitInfo = circlePanel.commit

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
            val modelService = commit.project.service<ModelService>()
            if (e.isShiftDown) {
                shiftClick()
                println("shift aint shifting")
                return
            } else if (e.isMetaDown || e.isControlDown) {
                controlClick()
                println("CTRL")
                return
            }
            if (e.button == MouseEvent.BUTTON1) {
                if (!circlePanel.commit.isSelected || modelService.branchInfo.getActualSelectedCommitsSize() > 1) {
                    modelService.selectSingleCommit(circlePanel.commit)
                } else {
                    modelService.removeFromSelectedCommits(circlePanel.commit)
                    println("deselect commit")
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
            invokePopup(e.component, e.x, e.y)
            e.consume()
        }
    }

    /**
     * Open context menu
     */
    override fun mouseReleased(e: MouseEvent?) {
        if (e != null && e.isPopupTrigger) {
            invokePopup(e.component, e.x, e.y)
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

        modelService.selectSingleCommit(selectedCommit)

        modelService.getCurrentCommits()
            .subList(Integer.min(selectedIndex, commitIndex), Integer.max(selectedIndex + 1, commitIndex + 1))
            .forEach {
                if (it != selectedCommit) {
                    modelService.addToSelectedCommits(it)
                }
            }
    }

    /**
     * Adds to the selected commits
     * the currently selected commit
     */
    private fun controlClick() {
        val modelService = commit.project.service<ModelService>()
        if (!commit.isSelected) {
            modelService.addToSelectedCommits(commit)
        } else {
            modelService.removeFromSelectedCommits(commit)
        }
    }

    override fun invokePopup(
        comp: Component?,
        x: Int,
        y: Int,
    ) {
        val popupMenu = actionManager.createActionPopupMenu(ActionPlaces.EDITOR_TAB, actionsGroup)
        popupMenu.component.show(comp, x, y)
    }

    override fun dispose() {
    }
}
