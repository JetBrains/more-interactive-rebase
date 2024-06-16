package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.visuals.multipleBranches.SideBranchPanel
import com.jetbrains.interactiveRebase.visuals.multipleBranches.SidePanel
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import javax.swing.SwingUtilities
import kotlin.math.max
import kotlin.math.min

class SidePanelListener(val project: Project, val sidePanel: SidePanel) : KeyListener, Disposable {
    var selected: SideBranchPanel? = null

    override fun keyPressed(e: KeyEvent?) {
        when (e?.keyCode) {
            KeyEvent.VK_UP -> up()
            KeyEvent.VK_DOWN -> down()
            KeyEvent.VK_RIGHT -> right()
            KeyEvent.VK_ENTER -> enter()
            KeyEvent.VK_ESCAPE -> escape()
        }
        e?.consume()
    }

    internal fun up() {
        if (selected?.isSelected!!) return
        val index = max(sidePanel.sideBranchPanels.indexOf(selected) - 1, 0)
        sidePanel.resetAllBranchesVisually()
        selected = sidePanel.sideBranchPanels[index]
        selected!!.onHover()
    }

    internal fun down() {
        if (selected?.isSelected!!) return
        val index = min(sidePanel.sideBranchPanels.indexOf(selected) + 1, sidePanel.sideBranchPanels.size - 1)
        sidePanel.resetAllBranchesVisually()
        selected = sidePanel.sideBranchPanels[index]
        selected!!.onHover()
    }

    internal fun right() {
        val actionService = project.service<ActionService>()
        val mainPanel = actionService.mainPanel

        sidePanel.background = mainPanel.background

        SwingUtilities.invokeLater { mainPanel.requestFocusInWindow() }
    }

    internal fun enter() {
        if (selected?.isSelected!!) return
        sidePanel.selectBranch(selected!!)
    }

    internal fun escape() {
        if (!selected?.isSelected!!) return
        selected?.deselectBranch()
        sidePanel.resetAllBranchesVisually()
        selected!!.onHover()
    }

    override fun keyTyped(e: KeyEvent?) {
    }

    override fun keyReleased(e: KeyEvent?) {
    }

    override fun dispose() {
    }
}
