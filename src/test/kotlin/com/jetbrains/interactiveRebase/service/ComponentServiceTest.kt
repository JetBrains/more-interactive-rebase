package com.jetbrains.interactiveRebase.service

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.OnePixelSplitter
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.services.ComponentService
import com.jetbrains.interactiveRebase.threads.CommitInfoThread
import com.jetbrains.interactiveRebase.visuals.LabeledBranchPanel
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.awt.BorderLayout
import java.awt.GridBagLayout

class ComponentServiceTest : BasePlatformTestCase() {
    lateinit var componentService: ComponentService

    override fun setUp() {
        super.setUp()
        componentService = ComponentService(project)
    }

    fun testUpdateMainPanelVisuals() {
        componentService.updateMainPanelVisuals()

        assertEquals(1, componentService.mainComponent.componentCount)
        assertTrue(componentService.mainComponent.getComponent(0) is OnePixelSplitter)
    }

    fun testCreateMainComponent() {
        val mainComponent = componentService.createMainComponent()

        assertTrue(mainComponent.layout is BorderLayout)
    }

    fun testCreateBranchPanel() {
        val branchPanel = componentService.createBranchPanel()

        assertTrue(branchPanel.layout is GridBagLayout)
        assertEquals(1, branchPanel.componentCount)
        assertTrue(branchPanel.getComponent(0) is LabeledBranchPanel)
    }

    fun testAddOrRemoveCommitSelection() {
        val commit1 = mock(CommitInfo::class.java)
        `when`(commit1.isSelected).thenReturn(true)

        val res = componentService.addOrRemoveCommitSelection(commit1)
        assertEquals(componentService.branchInfo.selectedCommits, listOf(commit1))
    }

    fun testAddOrRemoveCommitSelectionCommitIsNotSelected() {
        val commit1 = mock(CommitInfo::class.java)
        `when`(commit1.isSelected).thenReturn(false)
        componentService.branchInfo.selectedCommits = mutableListOf(commit1)

        val res = componentService.addOrRemoveCommitSelection(commit1)
        assertEquals(componentService.branchInfo.selectedCommits.size, 0)
    }

    fun testGetSelectedCommits() {
        val commit1 = mock(CommitInfo::class.java)
        val commit2 = mock(CommitInfo::class.java)
        componentService.branchInfo.selectedCommits = mutableListOf(commit1, commit2)

        val res = componentService.getSelectedCommits()
        assertEquals(res, listOf(commit1, commit2))
    }

    fun testUpdateMainComponentThread() {
        val mockThread = mock(CommitInfoThread::class.java)
        doNothing().`when`(mockThread).join()
        doNothing().`when`(mockThread).start()

        val updated = componentService.updateMainComponentThread()

        assertEquals(1, updated.componentCount)
        assertEquals(OnePixelSplitter::class.java, updated.getComponent(0).javaClass)

        // TODO: Find a way to test the actual thread
    }
}
