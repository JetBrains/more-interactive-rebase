package com.jetbrains.interactiveRebase.dataClasses

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations.openMocks

class CommitInfoTest : BasePlatformTestCase() {
    private lateinit var commitInfo: CommitInfo
    private lateinit var listener: CommitInfo.Listener

    override fun setUp() {
        super.setUp()
        val commitProvider = TestGitCommitProvider(project)
        commitInfo = CommitInfo(commitProvider.createCommit("tests"), project, mutableListOf())
        listener =
            spy(
                object : CommitInfo.Listener {
                    override fun onCommitChange() {
                    }
                },
            )
        openMocks(this)
        commitInfo.addListener(listener)
    }

    fun testAddChange() {
        commitInfo.addChange(DropCommand(mutableListOf(commitInfo)))
        assertEquals(1, commitInfo.changes.size)
        verify(listener, times(1)).onCommitChange()
    }

    fun testSetSelectedTo() {
        commitInfo.setSelectedTo(true)
        assertTrue(commitInfo.isSelected)
    }

    fun testSetHoveredTo() {
        commitInfo.setHoveredTo(true)
        assertTrue(commitInfo.isHovered)
    }

    fun testSetDoubleClickTo() {
        commitInfo.setDoubleClickedTo(true)
        assertTrue(commitInfo.isDoubleClicked)
        verify(listener, times(1)).onCommitChange()
    }

    fun testFlipSelected() {
        commitInfo.setSelectedTo(false)
        commitInfo.flipSelected()
        assertTrue(commitInfo.isSelected)
    }

    fun testFlipHovered() {
        commitInfo.setHoveredTo(false)
        commitInfo.flipHovered()
        assertTrue(commitInfo.isHovered)
    }

    fun testFlipDoubleCLick() {
        commitInfo.setDoubleClickedTo(false)
        commitInfo.flipDoubleClicked()
        assertTrue(commitInfo.isDoubleClicked)
        verify(listener, times(2)).onCommitChange()
    }
}
