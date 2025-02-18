package com.jetbrains.interactiveRebase.listeners

import com.jetbrains.interactiveRebase.visuals.multipleBranches.SideBranchPanel
import com.jetbrains.interactiveRebase.visuals.multipleBranches.SidePanel
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

class RemoveSideBranchListener(val sideBranchPanel: SideBranchPanel, val parent: SidePanel) : MouseAdapter() {
    /**
     * Removes the selection of the branch from the panel
     * when the mouse is clicked on the button.
     */
    override fun mouseClicked(e: MouseEvent?) {
        if (sideBranchPanel.deselectBranch()) parent.resetAllBranchesVisually()
        e?.consume()
    }

    /**
     * Changes the color of the button when the mouse hovers on top of it.
     */
    override fun mouseEntered(e: MouseEvent?) {
        sideBranchPanel.buttonOnHover()
        e?.consume()
    }

    /**
     * Resets the color of the button when the mouse exits the button.
     */
    override fun mouseExited(e: MouseEvent?) {
        sideBranchPanel.buttonOnHoverExit()
        e?.consume()
    }
}
