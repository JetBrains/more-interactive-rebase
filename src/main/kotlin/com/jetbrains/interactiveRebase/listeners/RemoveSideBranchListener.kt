package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.Disposable
import com.jetbrains.interactiveRebase.visuals.multipleBranches.SideBranchPanel
import com.jetbrains.interactiveRebase.visuals.multipleBranches.SidePanel
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

class RemoveSideBranchListener(val sideBranchPanel: SideBranchPanel, val parent: SidePanel): MouseAdapter(), Disposable {
    override fun mouseClicked(e: MouseEvent?) {
        if(sideBranchPanel.isSelected)
            parent.resetAllBranchesVisually()
    }

    override fun mouseEntered(e: MouseEvent?){
        sideBranchPanel.buttonOnHover()
    }

    override fun mouseExited(e: MouseEvent?){
        sideBranchPanel.buttonOnHoverExit()
    }
    override fun dispose() {
    }
}