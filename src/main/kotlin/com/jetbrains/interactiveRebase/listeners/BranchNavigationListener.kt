package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.services.ModelService
import java.awt.event.KeyEvent
import java.awt.event.KeyListener

class BranchNavigationListener(project: Project) : KeyListener, Disposable {
    private val modelService: ModelService = project.service<ModelService>()

    /**
     * Changes selected commit on
     * arrow key press
     */
    override fun keyPressed(e: KeyEvent?) {
        if(e?.isShiftDown!!){
            when (e.keyCode) {
                KeyEvent.VK_UP -> shiftUp()
                KeyEvent.VK_DOWN -> shiftDown()
            }
            return
        }

        when (e.keyCode) {
            KeyEvent.VK_UP -> up()
            KeyEvent.VK_DOWN -> down()
        }
    }

    /**
     * Moves through the branch
     * selecting one single commit,
     * moves up
     */
    private fun up() {
        if (modelService.branchInfo.selectedCommits.size == 0) {
            val commit = modelService.branchInfo.currentCommits.last()
            modelService.selectSingleCommit(commit)
            return
        }
        var commit = modelService.branchInfo.selectedCommits.last()

        val index = modelService.branchInfo.currentCommits.indexOf(commit)
        if(index == 0){
            return
        }

        commit = modelService.branchInfo.currentCommits[index - 1]
        modelService.selectSingleCommit(commit)
    }

    /**
     * Moves through the branch
     * selecting one single commit,
     * moves down
     */
    private fun down() {
        if (modelService.branchInfo.selectedCommits.size == 0) {
            val commit = modelService.branchInfo.currentCommits[0]
            modelService.selectSingleCommit(commit)
            return
        }
        var commit = modelService.branchInfo.selectedCommits.last()

        val index = modelService.branchInfo.currentCommits.indexOf(commit)
        if(index == modelService.branchInfo.currentCommits.size - 1){
            return
        }

        commit = modelService.branchInfo.currentCommits[index + 1]
        modelService.selectSingleCommit(commit)
    }

    /**
     * Moves through the branch
     * adding or removing commits
     * to the list of selected
     * commits, moves up
     */
    private fun shiftUp() {
        if (modelService.branchInfo.selectedCommits.size == 0) {
            val commit = modelService.branchInfo.currentCommits.last()
            modelService.addToSelectedCommits(commit)
            return
        }
        val commit = modelService.branchInfo.selectedCommits.last()

        val index = modelService.branchInfo.currentCommits.indexOf(commit)
        if(index == 0){
            return
        }

        val nextCommit = modelService.branchInfo.currentCommits[index - 1]
        if(!nextCommit.isSelected){
            modelService.addToSelectedCommits(nextCommit)
        }

        else{
            modelService.removeFromSelectedCommits(commit)
        }
        }

    /**
     * Moves through the branch
     * adding or removing commits
     * to the list of selected
     * commits, moves down
     */
    private fun shiftDown() {
        if (modelService.branchInfo.selectedCommits.size == 0) {
            val commit = modelService.branchInfo.currentCommits[0]
            modelService.addToSelectedCommits(commit)
            return
        }
        val commit = modelService.branchInfo.selectedCommits.last()

        val index = modelService.branchInfo.currentCommits.indexOf(commit)
        if(index == modelService.branchInfo.currentCommits.size - 1){
            return
        }

        val nextCommit = modelService.branchInfo.currentCommits[index + 1]
        if(!nextCommit.isSelected){
            modelService.addToSelectedCommits(nextCommit)
        }

        else{
            modelService.removeFromSelectedCommits(commit)
        }
    }

    override fun keyTyped(e: KeyEvent?) {
    }

    override fun keyReleased(e: KeyEvent?) {
    }

    override fun dispose() {
    }
}
