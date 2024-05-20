package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.services.ComponentService
import com.jetbrains.interactiveRebase.visuals.CommitInfoPanel
import git4idea.GitCommit
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.awt.event.MouseEvent
import javax.swing.JButton

class DropCommitListenerTest : BasePlatformTestCase() {
    private lateinit var button: JButton
    private lateinit var service: ComponentService
    private lateinit var listener: DropCommitListener
    private lateinit var commitInfo: CommitInfo
    private lateinit var branchInfo: BranchInfo

    override fun setUp() {
        super.setUp()
        button = JButton()
        service = project.service<ComponentService>()
        service.commitInfoPanel = mock(CommitInfoPanel::class.java)
        doNothing().`when`(service.commitInfoPanel).commitsSelected(anyCustom())
        doNothing().`when`(service.commitInfoPanel).repaint()
        commitInfo = CommitInfo(mock(GitCommit::class.java), project, mutableListOf(), true)
        branchInfo = BranchInfo("feature1", mutableListOf(commitInfo))
        service.branchInfo = branchInfo
        service.addOrRemoveCommitSelection(commitInfo)
        listener = DropCommitListener(button, project)
    }

    fun testMouseClicked() {
        val event = mock(MouseEvent::class.java)

        listener.mouseClicked(event)

        assertTrue(service.isDirty)
        assertTrue(service.branchInfo.selectedCommits.isEmpty())
        assertTrue(commitInfo.changes.isNotEmpty())
        assertFalse(commitInfo.isSelected)
        verify(service.commitInfoPanel).commitsSelected(anyCustom())
        verify(service.commitInfoPanel).repaint()
    }

    fun testMousePressed() {
        val event = mock(MouseEvent::class.java)

        listener.mousePressed(event)

        assertTrue(service.isDirty)
        assertTrue(service.branchInfo.selectedCommits.isEmpty())
        assertTrue(commitInfo.changes.isNotEmpty())
        assertFalse(commitInfo.isSelected)
        verify(service.commitInfoPanel).commitsSelected(anyCustom())
        verify(service.commitInfoPanel).repaint()
    }

    fun testMouseReleasedDoesNothing() {
        val event = mock(MouseEvent::class.java)
        listener.mouseReleased(event)
        assertNotEmpty(service.branchInfo.selectedCommits)
        assertFalse(service.isDirty)
        assertTrue(commitInfo.changes.isEmpty())
    }

    fun testMouseEnteredDoesNothing() {
        val event = mock(MouseEvent::class.java)
        listener.mouseReleased(event)
        assertNotEmpty(service.branchInfo.selectedCommits)
        assertTrue(commitInfo.changes.isEmpty())
    }

    fun testMouseExitedDoesNothing() {
        val event = mock(MouseEvent::class.java)
        listener.mouseReleased(event)
        assertNotEmpty(service.branchInfo.selectedCommits)
        assertTrue(commitInfo.changes.isEmpty())
    }

    private inline fun <reified T> anyCustom(): T = any(T::class.java)
}
