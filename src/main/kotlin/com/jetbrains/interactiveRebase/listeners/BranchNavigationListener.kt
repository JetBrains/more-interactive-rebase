package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.services.ModelService
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import kotlin.math.max
import kotlin.math.min

class BranchNavigationListener(project: Project, private val modelService: ModelService) : KeyListener, Disposable {
    constructor(project: Project) : this(project, project.service<ModelService>())

    /**
     * Changes selected commit on
     * arrow key press
     */
    override fun keyPressed(e: KeyEvent?) {
        if (e?.isShiftDown!!) {
            when (e.keyCode) {
                KeyEvent.VK_UP -> shiftUp()
                KeyEvent.VK_DOWN -> shiftDown()
            }
            e.consume()
            return
        }

        if (e.isAltDown) {
            when (e.keyCode) {
                KeyEvent.VK_UP -> altUp()
                KeyEvent.VK_DOWN -> altDown()
            }
            e.consume()
            return
        }

        when (e.keyCode) {
            KeyEvent.VK_UP -> up()
            KeyEvent.VK_DOWN -> down()
        }
        e.consume()
    }

    /**
     * Moves through the branch
     * selecting one single commit,
     * moves up
     */
    fun up() {
        if (modelService.branchInfo.selectedCommits.size == 0) {
            val commit = modelService.branchInfo.currentCommits.last()
            modelService.selectSingleCommit(commit, modelService.branchInfo)
            return
        }

        var commit = modelService.getLastSelectedCommit(modelService.branchInfo)

        val index = modelService.branchInfo.currentCommits.indexOf(commit)
        if (index == 0) {
            commit = modelService.branchInfo.currentCommits[index]
            modelService.selectSingleCommit(commit, modelService.branchInfo)
            return
        }

        commit = modelService.branchInfo.currentCommits[index - 1]
        if (commit.isCollapsed) {
            commit = modelService.branchInfo.currentCommits[index - 2]
        }
        modelService.selectSingleCommit(commit, modelService.branchInfo)
    }

    /**
     * Moves through the branch
     * selecting one single commit,
     * moves down
     */
    fun down() {
        if (modelService.branchInfo.selectedCommits.size == 0) {
            val commit = modelService.branchInfo.currentCommits[0]
            modelService.selectSingleCommit(commit, modelService.branchInfo)
            return
        }
        var commit = modelService.getLastSelectedCommit(modelService.branchInfo)

        val index = modelService.branchInfo.currentCommits.indexOf(commit)
        if (index == modelService.branchInfo.currentCommits.size - 1) {
            commit = modelService.branchInfo.currentCommits[index]
            modelService.selectSingleCommit(commit, modelService.branchInfo)
            return
        }

        commit = modelService.branchInfo.currentCommits[index + 1]
        if (commit.isCollapsed) {
            commit = modelService.branchInfo.currentCommits[index + 2]
        }
        modelService.selectSingleCommit(commit, modelService.branchInfo)
    }

    /**
     * Moves through the branch
     * adding or removing commits
     * to the list of selected
     * commits, moves up
     */
    fun shiftUp() {
        if (modelService.branchInfo.selectedCommits.size == 0) {
            val commit = modelService.branchInfo.currentCommits.last()
            modelService.addToSelectedCommits(commit, modelService.branchInfo)
            return
        }
        val commit = modelService.getLastSelectedCommit(modelService.branchInfo)

        val index = modelService.branchInfo.currentCommits.indexOf(commit)
        if (index == 0) {
            return
        }

        var nextCommit = modelService.branchInfo.currentCommits[index - 1]
        if (nextCommit.isCollapsed) {
            nextCommit = modelService.branchInfo.currentCommits[index - 2]
        }
        if (!nextCommit.isSelected) {
            modelService.addToSelectedCommits(nextCommit, modelService.branchInfo)
        } else {
            modelService.removeFromSelectedCommits(commit, modelService.branchInfo)
        }
    }

    /**
     * Moves through the branch
     * adding or removing commits
     * to the list of selected
     * commits, moves down
     */
    fun shiftDown() {
        if (modelService.branchInfo.selectedCommits.size == 0) {
            val commit = modelService.branchInfo.currentCommits[0]
            modelService.addToSelectedCommits(commit, modelService.branchInfo)
            return
        }
        val commit = modelService.getLastSelectedCommit(modelService.branchInfo)

        val index = modelService.branchInfo.currentCommits.indexOf(commit)
        if (index == modelService.branchInfo.currentCommits.size - 1) {
            return
        }

        var nextCommit = modelService.branchInfo.currentCommits[index + 1]
        if (nextCommit.isCollapsed) {
            nextCommit = modelService.branchInfo.currentCommits[index + 2]
        }
        if (!nextCommit.isSelected) {
            modelService.addToSelectedCommits(nextCommit, modelService.branchInfo)
        } else {
            modelService.removeFromSelectedCommits(commit, modelService.branchInfo)
        }
    }

    /**
     * Moves through the branch
     * adding or removing commits
     * to the list of selected
     * commits, moves up
     */
    fun altUp() {
        modelService.getSelectedCommits().sortBy { modelService.branchInfo.indexOfCommit(it) }
        modelService.getSelectedCommits().forEach {
                commit ->
            if (!commit.isSquashed) {
                val oldIndex = modelService.branchInfo.currentCommits.indexOf(commit)
                val newIndex = max(oldIndex - 1, 0)

                modelService.markCommitAsReordered(commit, oldIndex, newIndex)
                modelService.branchInfo.updateCurrentCommits(oldIndex, newIndex, commit)
            }
        }
    }

    /**
     * Moves through the branch
     * adding or removing commits
     * to the list of selected
     * commits, moves down
     */
    fun altDown() {
        modelService.getSelectedCommits().sortBy { modelService.branchInfo.indexOfCommit(it) }
        modelService.getSelectedCommits().reversed().forEach {
                commit ->
            if (!commit.isSquashed) {
                val oldIndex = modelService.branchInfo.currentCommits.indexOf(commit)
                val newIndex = min(oldIndex + 1, modelService.branchInfo.currentCommits.size - 1)

                modelService.markCommitAsReordered(commit, oldIndex, newIndex)
                modelService.branchInfo.updateCurrentCommits(oldIndex, newIndex, commit)
            }
        }
    }

    override fun keyTyped(e: KeyEvent?) {
    }

    override fun keyReleased(e: KeyEvent?) {
    }

    override fun dispose() {
    }
}
