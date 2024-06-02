package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.OnePixelSplitter
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.awt.BorderLayout

class MainPanelTest : BasePlatformTestCase() {
    private lateinit var modelService: ModelService
    private lateinit var branchInfo: BranchInfo
    private lateinit var mainPanel: MainPanel

    override fun setUp() {
        super.setUp()

        modelService = ModelService(project, CoroutineScope(Dispatchers.Default))
        branchInfo = modelService.branchInfo
        mainPanel = MainPanel(project, branchInfo)
//        mainPanel = MainPanel(project, branchInfo, branchInfo)
    }

    fun testUpdateMainPanelVisuals() {
        assertEquals(1, mainPanel.componentCount)
        assertTrue(mainPanel.getComponent(0) is OnePixelSplitter)
    }

    fun testCreateMainComponent() {
        assertTrue(mainPanel.layout is BorderLayout)
    }

    fun testAddOrRemoveCommitSelection() {
        val commit1 = CommitInfo(TestGitCommitProvider(project).createCommit("my commit"), project, mutableListOf())
        commit1.isSelected = true

        modelService.addOrRemoveCommitSelection(commit1)
        assertEquals(modelService.branchInfo.selectedCommits, listOf(commit1))
    }

    fun testAddOrRemoveCommitSelectionCommitIsNotSelected() {
        val commit1 = mock(CommitInfo::class.java)
        `when`(commit1.isSelected).thenReturn(false)
        modelService.branchInfo.selectedCommits = mutableListOf(commit1)

        modelService.addOrRemoveCommitSelection(commit1)
        assertEquals(modelService.branchInfo.selectedCommits.size, 0)
    }

    fun testGetSelectedCommits() {
        val commit1 = mock(CommitInfo::class.java)
        val commit2 = mock(CommitInfo::class.java)
        modelService.branchInfo.selectedCommits = mutableListOf(commit1, commit2)

        val res = modelService.getSelectedCommits()
        assertEquals(res, listOf(commit1, commit2))
    }

    fun testUpdateMainComponentThread() {
        val mockService = mock(ModelService::class.java)
        doNothing().`when`(mockService).fetchGraphInfo()

        assertEquals(1, mainPanel.componentCount)
        assertEquals(OnePixelSplitter::class.java, mainPanel.getComponent(0).javaClass)
    }
}
