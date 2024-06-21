package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.ModelService
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import kotlin.math.max
import kotlin.math.min

class BranchNavigationListener(val project: Project, private val modelService: ModelService) : KeyListener, Disposable {
    constructor(project: Project) : this(project, project.service<ModelService>())

    /**
     * Changes selected commit on
     * arrow key press
     */
    override fun keyPressed(e: KeyEvent?) {
        if(e == null) return

        if (e.keyCode == KeyEvent.VK_ESCAPE) {
            modelService.clearSelectedCommits()
        }

        if (e.isShiftDown) {
            when (e.keyCode) {
                KeyEvent.VK_UP -> shiftUp()
                KeyEvent.VK_DOWN -> shiftDown()
                KeyEvent.VK_RIGHT -> right()
                KeyEvent.VK_LEFT -> left()
            }
            e.consume()
            return
        }

        if (e.isAltDown) {
            when (e.keyCode) {
                KeyEvent.VK_UP -> altUp()
                KeyEvent.VK_DOWN -> altDown()
                KeyEvent.VK_RIGHT -> right()
                KeyEvent.VK_LEFT -> left()
            }
            e.consume()
            return
        }

        when (e.keyCode) {
            KeyEvent.VK_UP -> up()
            KeyEvent.VK_DOWN -> down()
            KeyEvent.VK_RIGHT -> right()
            KeyEvent.VK_LEFT -> left()
        }
        e.consume()
    }

    /**
     * Moves through the branch
     * selecting one single commit,
     * moves up
     */
    fun up() {
        val branch = modelService.getSelectedBranch()
        if (branch.selectedCommits.size == 0) {
            val commit = branch.currentCommits.last()
            modelService.selectSingleCommit(commit, branch)
            return
        }

        var commit = modelService.getLastSelectedCommit()

        val index = branch.currentCommits.indexOf(commit)
        if (index == 0) {
            commit = branch.currentCommits[index]
            modelService.selectSingleCommit(commit, branch)
            return
        }

        commit = branch.currentCommits[index - 1]
        if (commit.isCollapsed) {
            commit = branch.currentCommits[index - 2]
        }
        modelService.selectSingleCommit(commit, branch)
    }

    /**
     * Moves through the branch
     * selecting one single commit,
     * moves down
     */
    fun down() {
        val branch = modelService.getSelectedBranch()
        if (branch.selectedCommits.size == 0) {
            val commit = branch.currentCommits[0]
            modelService.selectSingleCommit(commit, branch)
            return
        }
        var commit = modelService.getLastSelectedCommit()

        val index = branch.currentCommits.indexOf(commit)
        if (index == branch.currentCommits.size - 1) {
            commit = branch.currentCommits[index]
            modelService.selectSingleCommit(commit, branch)
            return
        }

        commit = branch.currentCommits[index + 1]
        if (commit.isCollapsed) {
            commit = branch.currentCommits[index + 2]
        }
        modelService.selectSingleCommit(commit, branch)
    }

    /**
     * Moves through the branch
     * adding or removing commits
     * to the list of selected
     * commits, moves up
     */
    fun shiftUp() {
        val branch = modelService.getSelectedBranch()
        if (branch.selectedCommits.size == 0) {
            val commit = branch.currentCommits.last()
            modelService.addToSelectedCommits(commit, branch)
            return
        }
        val commit = modelService.getLastSelectedCommit()

        val index = branch.currentCommits.indexOf(commit)
        if (index == 0) {
            return
        }

        var nextCommit = branch.currentCommits[index - 1]
        if (nextCommit.isCollapsed) {
            nextCommit = branch.currentCommits[index - 2]
        }
        if (!nextCommit.isSelected) {
            modelService.addToSelectedCommits(nextCommit, branch)
        } else {
            modelService.removeFromSelectedCommits(commit, branch)
        }
    }

    /**
     * Moves through the branch
     * adding or removing commits
     * to the list of selected
     * commits, moves down
     */
    fun shiftDown() {
        val branch = modelService.getSelectedBranch()
        if (branch.selectedCommits.size == 0) {
            val commit = branch.currentCommits[0]
            modelService.addToSelectedCommits(commit, branch)
            return
        }
        val commit = modelService.getLastSelectedCommit()

        val index = branch.currentCommits.indexOf(commit)
        if (index == branch.currentCommits.size - 1) {
            return
        }

        var nextCommit = branch.currentCommits[index + 1]
        if (nextCommit.isCollapsed) {
            nextCommit = branch.currentCommits[index + 2]
        }
        if (!nextCommit.isSelected) {
            modelService.addToSelectedCommits(nextCommit, branch)
        } else {
            modelService.removeFromSelectedCommits(commit, branch)
        }
    }

    /**
     * Moves through the branch
     * adding or removing commits
     * to the list of selected
     * commits, moves up
     */
    fun altUp() {
        if (modelService.areDisabledCommitsSelected()) return

        val branch = modelService.getSelectedBranch()
        modelService.getSelectedCommits().sortBy { branch.indexOfCommit(it) }
        modelService.getSelectedCommits().forEach {
                commit ->
            if (!commit.isSquashed) {
                val oldIndex = branch.currentCommits.indexOf(commit)
                val newIndex = max(oldIndex - 1, 0)

                modelService.markCommitAsReordered(commit, oldIndex, newIndex)
                branch.updateCurrentCommits(oldIndex, newIndex, commit)
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
        if (modelService.areDisabledCommitsSelected()) return

        val branch = modelService.getSelectedBranch()
        modelService.getSelectedCommits().sortBy { branch.indexOfCommit(it) }
        modelService.getSelectedCommits().reversed().forEach {
                commit ->
            if (!commit.isSquashed) {
                val oldIndex = branch.currentCommits.indexOf(commit)
                val newIndex = min(oldIndex + 1, branch.currentCommits.size - 1)

                modelService.markCommitAsReordered(commit, oldIndex, newIndex)
                branch.updateCurrentCommits(oldIndex, newIndex, commit)
            }
        }
    }

    fun right() {
        val branch = modelService.getSelectedBranch()

        if (modelService.graphInfo.addedBranch == null ||
            modelService.getSelectedCommits().isEmpty() ||
            branch == modelService.graphInfo.addedBranch
        ) {
            return
        }

        modelService.selectSingleCommit(
            modelService.graphInfo.addedBranch!!.currentCommits.last(),
            modelService.graphInfo.addedBranch!!,
        )
    }

    fun left() {
        val branch = modelService.getSelectedBranch()

        if (branch == modelService.graphInfo.addedBranch) {
            modelService.selectSingleCommit(
                modelService.graphInfo.mainBranch.currentCommits.last(),
                modelService.graphInfo.mainBranch,
            )
        } else if (branch == modelService.graphInfo.mainBranch) {
            val actionService = project.service<ActionService>()
            actionService.mainPanel.sidePanel.selectPanel()
        }
    }

    override fun keyTyped(e: KeyEvent?) {
    }

    override fun keyReleased(e: KeyEvent?) {
    }

    override fun dispose() {
    }
}
