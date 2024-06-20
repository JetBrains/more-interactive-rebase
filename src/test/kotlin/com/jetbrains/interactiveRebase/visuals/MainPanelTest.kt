package com.jetbrains.interactiveRebase.visuals

import ai.grazie.detector.ngram.main
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.labels.BoldLabel
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider

import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.DialogService

import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.visuals.multipleBranches.RoundedPanel
import junit.framework.TestCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.mockito.Mockito.*
import java.awt.BorderLayout

class MainPanelTest : BasePlatformTestCase() {
    private lateinit var actionService: ActionService
    private lateinit var modelService: ModelService
    private lateinit var branchInfo: BranchInfo
    private lateinit var mainPanel: MainPanel

    override fun setUp() {
        super.setUp()
        actionService = ActionService(project)
        modelService = ModelService(project, CoroutineScope(Dispatchers.Default))
        branchInfo = modelService.branchInfo
        mainPanel = MainPanel(project)

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

        modelService.addToSelectedCommits(commit1, modelService.branchInfo)
        assertEquals(modelService.branchInfo.selectedCommits, listOf(commit1))
    }

    fun testAddOrRemoveCommitSelectionCommitIsNotSelected() {
        val commit1 = mock(CommitInfo::class.java)
        `when`(commit1.isSelected).thenReturn(false)
        modelService.branchInfo.selectedCommits = mutableListOf(commit1)

        modelService.removeFromSelectedCommits(commit1, modelService.branchInfo)
        assertEquals(modelService.branchInfo.selectedCommits.size, 0)
    }

    fun testGetSelectedCommits() {
        val commit1 = mock(CommitInfo::class.java)
        val commit2 = mock(CommitInfo::class.java)
        modelService.branchInfo.selectedCommits = mutableListOf(commit1, commit2)

        val res = modelService.getSelectedCommits()
        assertEquals(res, listOf(commit1, commit2))
    }



//    fun testOnNameChange() {
//        val mockDialogService = mock(DialogService::class.java)
//        `when`(mockDialogService
//            .warningOkCancelDialog("Git Issue", "test description"))
//            .thenReturn(true)
//        modelService.showWarningGitDialogClosesPlugin("test description", mockDialogService)
//
//        modelService.branchInfo.name = "branch2"
//
//        mainPanel.sidePanel.updateBranchNames()
//        mainPanel.graphPanel.mainBranchPanel.updateBranchName()
//
//        val res1 = mainPanel.sidePanel.sideBranchPanels.any {
//            it.branchName == "branch2"
//        }
//
//        val res2 = ((mainPanel
//            .graphPanel
//            .mainBranchPanel
//            .branchNamePanel
//            .getComponent(0) as RoundedPanel)
//            .getComponent(0) as BoldLabel)
//            .text == "branch2"
//
//
//        TestCase.assertTrue(res2)
//    }


    fun testUpdateMainComponentThread() {
        val mockService = mock(ModelService::class.java)
        doNothing().`when`(mockService).fetchGraphInfo(0)
        assertEquals(1, mainPanel.componentCount)
        assertEquals(OnePixelSplitter::class.java, mainPanel.getComponent(0).javaClass)
    }
}
