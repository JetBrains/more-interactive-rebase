package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.Disposable
import com.jetbrains.interactiveRebase.visuals.multipleBranches.SideBranchPanel
import com.jetbrains.interactiveRebase.visuals.multipleBranches.SidePanel
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

class SideBranchPanelListener(val sideBranchPanel: SideBranchPanel, val parent: SidePanel): MouseAdapter(), Disposable {

    override fun mouseEntered(e: MouseEvent?) {
        if(parent.canSelectBranch(sideBranchPanel))
            sideBranchPanel.onHover()
    }

    override fun mouseExited(e: MouseEvent?) {
        if(parent.canSelectBranch(sideBranchPanel))
            parent.resetAllBranchesVisually()
    }

    override fun mouseClicked(e: MouseEvent?) {
        if(parent.canSelectBranch(sideBranchPanel)){
            sideBranchPanel.selectBranch()
            parent.makeBranchesUnavailableExceptCurrent(sideBranchPanel)
        }
        else
            if(sideBranchPanel.isSelected)
                parent.resetAllBranchesVisually()
    }


    override fun mouseMoved(e: MouseEvent?) {
    }

    override fun dispose() {
    }

}