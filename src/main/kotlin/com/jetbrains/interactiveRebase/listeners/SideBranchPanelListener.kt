package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.Disposable
import com.jetbrains.interactiveRebase.visuals.multipleBranches.SideBranchPanel
import com.jetbrains.interactiveRebase.visuals.multipleBranches.SidePanel
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

class SideBranchPanelListener(val sideBranchPanel: SideBranchPanel, val parent: SidePanel) : MouseAdapter(), Disposable {
    /**
     * Defines the behavior of the panel when the mouse hovers on top of the panel.
     */
    override fun mouseEntered(e: MouseEvent?) {
        if (parent.canSelectBranch(sideBranchPanel)) {
            sideBranchPanel.onHover()
        }
        e?.consume()
    }

    /**
     * Defines the behavior of the panel when the mouse exits the panel, when it's hovered.
     */
    override fun mouseExited(e: MouseEvent?) {
        if (parent.canSelectBranch(sideBranchPanel)) {
            parent.resetAllBranchesVisually()
        }
        e?.consume()
    }

    /**
     * Defines the behavior of the panel when the mouse clicks on the panel.
     * It can select a branch if none are selected, or reset the selection if the branch is already selected.
     */
    override fun mouseClicked(e: MouseEvent?) {
        if (parent.canSelectBranch(sideBranchPanel)) {
            sideBranchPanel.selectBranch()
            parent.makeBranchesUnavailableExceptCurrent(sideBranchPanel)
        } else if (sideBranchPanel.isSelected) {
            // We possibly don't want to unselect the branch only by a single click on the panel, TBD
            parent.resetAllBranchesVisually()
        }
        e?.consume()
    }

    override fun dispose() {
    }
}
