package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.application.EDT
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.visuals.CommitInfoPanel
import com.jetbrains.interactiveRebase.visuals.MainPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.awt.event.MouseEvent
import javax.swing.JButton

class DropCommitListenerTest : BasePlatformTestCase() {
    private lateinit var mainPanel: MainPanel
    private lateinit var modelService: ModelService
    private lateinit var button: JButton
    private lateinit var listener: DropCommitListener
    private lateinit var commitInfo: CommitInfo
    private lateinit var branchInfo: BranchInfo
    private lateinit var event: MouseEvent

    override fun setUp() {
        super.setUp()
        val commitProvider = TestGitCommitProvider(project)
        commitInfo = CommitInfo(commitProvider.createCommit("tests"), project, mutableListOf())

        val commitService = mock(CommitService::class.java)

        doAnswer {
            listOf(commitInfo.commit)
        }.`when`(commitService).getCommits()

        doAnswer {
            "feature1"
        }.`when`(commitService).getBranchName()

        modelService = ModelService(project, CoroutineScope(Dispatchers.EDT), commitService)
        modelService.addOrRemoveCommitSelection(commitInfo)
        modelService.branchInfo.setName("feature1")
        modelService.branchInfo.addSelectedCommits(commitInfo)
        branchInfo = modelService.branchInfo
        mainPanel = MainPanel(project, branchInfo, modelService.invoker)
        button = JButton()
        mainPanel.commitInfoPanel = mock(CommitInfoPanel::class.java)
        doNothing().`when`(mainPanel.commitInfoPanel).commitsSelected(anyCustom())
        doNothing().`when`(mainPanel.commitInfoPanel).repaint()
        listener = DropCommitListener(modelService, button, project, modelService.invoker)
        event = MouseEvent(button, 0, 0, 0, 0, 0, 0, false)
    }

    fun testMouseClicked() {
        listener.mouseClicked(event)

        assertTrue(branchInfo.selectedCommits.isEmpty())
        assertTrue(commitInfo.changes.isNotEmpty())
        assertFalse(commitInfo.isSelected)
        verify(mainPanel.commitInfoPanel).commitsSelected(anyCustom())
    }

    fun testMousePressed() {
        listener.mousePressed(event)

        assertTrue(branchInfo.selectedCommits.isEmpty())
        assertTrue(commitInfo.changes.isNotEmpty())
        assertFalse(commitInfo.isSelected)
        verify(mainPanel.commitInfoPanel).commitsSelected(anyCustom())
    }

    fun testMouseReleasedDoesNothing() {
        listener.mouseReleased(event)
        assertNotEmpty(branchInfo.selectedCommits)
        assertTrue(commitInfo.changes.isEmpty())
    }

    fun testMouseEnteredDoesNothing() {
        listener.mouseReleased(event)
        assertNotEmpty(branchInfo.selectedCommits)
        assertTrue(commitInfo.changes.isEmpty())
    }

    fun testMouseExitedDoesNothing() {
        listener.mouseReleased(event)
        assertNotEmpty(branchInfo.selectedCommits)
        assertTrue(commitInfo.changes.isEmpty())
    }

    private inline fun <reified T> anyCustom(): T = any(T::class.java)
}
