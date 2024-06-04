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
        commitInfo.addChange(DropCommand(commitInfo))
        assertEquals(1, commitInfo.changes.size)
        verify(listener, times(1)).onCommitChange()
    }

    fun testSetDoubleClickTo() {
        commitInfo.setTextFieldEnabledTo(true)
        assertTrue(commitInfo.isTextFieldEnabled)
        verify(listener, times(1)).onCommitChange()
    }

    fun testFlipSelected() {
        commitInfo.isSelected = false
        commitInfo.flipSelected()
        assertTrue(commitInfo.isSelected)
    }

    fun testFlipHovered() {
        commitInfo.isHovered = false
        commitInfo.flipHovered()
        assertTrue(commitInfo.isHovered)
    }

    fun testFlipDoubleCLick() {
        commitInfo.setTextFieldEnabledTo(false)
        commitInfo.flipDoubleClicked()
        assertTrue(commitInfo.isTextFieldEnabled)
        verify(listener, times(2)).onCommitChange()
    }

    fun testSetReorderTo() {
        commitInfo.setReorderedTo(false)
        assertFalse(commitInfo.isReordered)
        verify(listener, times(1)).onCommitChange()
    }

    fun testSetDraggedTo() {
        commitInfo.isDragged = false
        assertFalse(commitInfo.isDragged)
    }

    fun testToString() {
        assertEquals(commitInfo.toString(), "CommitInfo(commit=tests)")
    }
}
