package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.visuals.multipleBranches.SideBranchPanel
import com.jetbrains.interactiveRebase.visuals.multipleBranches.SidePanel
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import kotlin.math.max
import kotlin.math.min

class SidePanelListener(val project: Project, val sidePanel: SidePanel): KeyListener, Disposable {
    var selected: SideBranchPanel? = null

    override fun keyPressed(e: KeyEvent?) {
        when (e?.keyCode) {
            KeyEvent.VK_UP -> up()
            KeyEvent.VK_DOWN -> down()
        }
        e?.consume()
    }

    fun up(){
        val index = max(sidePanel.sideBranchPanels.indexOf(selected) - 1, 0)
        sidePanel.resetAllBranchesVisually()
        selected = sidePanel.sideBranchPanels[index]
        selected!!.onHover()
    }

    fun down() {
        val index = min(sidePanel.sideBranchPanels.indexOf(selected) + 1, sidePanel.sideBranchPanels.size - 1)
        sidePanel.resetAllBranchesVisually()
        selected = sidePanel.sideBranchPanels[index]
        selected!!.onHover()
    }

    override fun keyTyped(e: KeyEvent?) {
    }

    override fun keyReleased(e: KeyEvent?) {
    }

    override fun dispose() {
    }
}