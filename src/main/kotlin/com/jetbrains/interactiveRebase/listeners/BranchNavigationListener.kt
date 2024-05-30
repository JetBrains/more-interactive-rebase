package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.services.ModelService
import java.awt.event.KeyEvent
import java.awt.event.KeyListener

class BranchNavigationListener(private val project: Project) : KeyListener, Disposable {
    private val modelService: ModelService = project.service<ModelService>()

    /**
     * Changes selected commit on
     * arrow key press
     */
    override fun keyPressed(e: KeyEvent?) {
        when (e?.keyCode) {
            KeyEvent.VK_UP -> up()
            KeyEvent.VK_DOWN -> down()
        }
    }

    private fun up() {
        if (modelService.branchInfo.selectedCommits.size == 0) {
            val commit = modelService.branchInfo.currentCommits.last()
            commit.isSelected = true
            modelService.addOrRemoveCommitSelection(commit)
            return
        }
        var commit = modelService.branchInfo.selectedCommits.last()

        val index = modelService.branchInfo.currentCommits.indexOf(commit)
        modelService.branchInfo.clearSelectedCommits()
        commit = modelService.branchInfo.currentCommits[index - 1]
        commit.isSelected = true
        modelService.addOrRemoveCommitSelection(commit)
    }

    private fun down() {
        if (modelService.branchInfo.selectedCommits.size == 0) {
            val commit = modelService.branchInfo.currentCommits[0]
            commit.isSelected = true
            modelService.addOrRemoveCommitSelection(commit)
            return
        }
        var commit = modelService.branchInfo.selectedCommits.last()

        val index = modelService.branchInfo.currentCommits.indexOf(commit)
        modelService.branchInfo.clearSelectedCommits()
        commit = modelService.branchInfo.currentCommits[index + 1]
        commit.isSelected = true
        modelService.addOrRemoveCommitSelection(commit)
    }

    override fun keyTyped(e: KeyEvent?) {
    }

    override fun keyReleased(e: KeyEvent?) {
    }

    override fun dispose() {
    }
}
