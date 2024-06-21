package com.jetbrains.interactiveRebase.dataClasses

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.FixupCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.PickCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.RewordCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.StopToEditCommand
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertNotEquals
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations.openMocks

class BranchInfoTest : BasePlatformTestCase() {
    private lateinit var branchInfo: BranchInfo
    private lateinit var listener: BranchInfo.Listener
    private lateinit var commit: CommitInfo
    private lateinit var commit1: CommitInfo

    override fun setUp() {
        super.setUp()
        branchInfo = BranchInfo()
        val commitProvider = TestGitCommitProvider(project)
        commit = CommitInfo(commitProvider.createCommit("tests"), project, mutableListOf())
        commit1 = CommitInfo(commitProvider.createCommit("tests1"), project, mutableListOf())

        listener =
            spy(
                object : BranchInfo.Listener {
                    override fun onNameChange(newName: String) {
                    }

                    override fun onCommitChange(commits: List<CommitInfo>) {
                    }

                    override fun onSelectedCommitChange(selectedCommits: MutableList<CommitInfo>) {
                    }

                    override fun onCurrentCommitsChange(currentCommits: MutableList<CommitInfo>) {
                    }
                },
            )
        openMocks(this)
        branchInfo.addListener(listener)
    }

    fun testSetName() {
        branchInfo.setName("newName")
        assertEquals("newName", branchInfo.name)
        verify(listener, times(1)).onNameChange("newName")
    }

    fun testSetCommits() {
        branchInfo.setCommits(listOf(commit))
        assertEquals(listOf(commit), branchInfo.currentCommits)
        verify(listener, times(1)).onCommitChange(listOf(commit))
    }

    fun testUpdateCurrentCommits() {
        branchInfo.setCommits(listOf(commit, commit1))
        branchInfo.updateCurrentCommits(1, 0, commit)
        assertEquals(branchInfo.currentCommits[0], commit)
        verify(listener, times(1)).onCurrentCommitsChange(branchInfo.currentCommits)
    }

    fun testGetIndexOfCommitsSquash() {
        branchInfo.setCommits(listOf(commit1, commit))
        commit.changes.add(StopToEditCommand(commit))
        commit.changes.add(PickCommand(commit))
        commit.changes.add(RewordCommand(commit, "commity"))
        commit.changes.add(SquashCommand(commit, mutableListOf(commit1), "commities"))
        commit.isSquashed = true

        assertEquals(branchInfo.indexOfCommit(commit), 0)
    }

    fun testGetIndexOfCommitsFixup() {
        branchInfo.setCommits(listOf(commit1, commit))
        commit.changes.add(StopToEditCommand(commit))
        commit.changes.add(PickCommand(commit))
        commit.changes.add(RewordCommand(commit, "commity"))
        commit.changes.add(FixupCommand(commit, mutableListOf(commit1)))
        commit.isSquashed = true

        assertEquals(branchInfo.indexOfCommit(commit), 0)
    }

    fun testRemoveSelectedCommitsWithSquashed() {
        branchInfo.setCommits(listOf(commit, commit1))
        commit.addChange(SquashCommand(commit, mutableListOf(commit1), "squash"))
        commit.addChange(FixupCommand(commit, mutableListOf(commit1)))
        branchInfo.selectedCommits.add(commit)
        branchInfo.selectedCommits.add(commit1)
        branchInfo.removeSelectedCommits(commit)
        assertEquals(0, branchInfo.selectedCommits.size)
    }

    fun testEquals() {
        val branchInfo1 = BranchInfo("branch", listOf(commit), mutableListOf(commit), true, true, false)
        val branchInfo2 = BranchInfo("branch", listOf(commit), mutableListOf(commit), true, true, false)
        assertEquals(branchInfo1, branchInfo2)
    }

    fun testEqualsDifferentName() {
        val branchInfo1 = BranchInfo("branch1", listOf(commit), mutableListOf(commit), true, true, false)
        val branchInfo2 = BranchInfo("branch2", listOf(commit), mutableListOf(commit), true, true, false)
        assertNotEquals(branchInfo1, branchInfo2)
    }

    fun testEqualsDifferentInitialCommits() {
        val branchInfo1 = BranchInfo("branch", listOf(commit1), mutableListOf(commit1), true, true, false)
        val branchInfo2 = BranchInfo("branch", listOf(commit), mutableListOf(commit), true, true, false)
        assertNotEquals(branchInfo1, branchInfo2)
    }

    fun testEqualsDifferentSelectedCommits() {
        val branchInfo1 = BranchInfo("branch", listOf(commit), mutableListOf(commit), true, true, false)
        val branchInfo2 = BranchInfo("branch", listOf(commit), mutableListOf(), true, true, false)
        assertNotEquals(branchInfo1, branchInfo2)
    }

    fun testEqualsNotBothPrimary() {
        val branchInfo1 = BranchInfo("branch", listOf(commit), mutableListOf(commit), true, true, false)
        val branchInfo2 = BranchInfo("branch", listOf(commit), mutableListOf(commit), false, true, false)
        assertNotEquals(branchInfo1, branchInfo2)
    }

    fun testEqualsNotSameCurrentCommits() {
        val branchInfo1 = BranchInfo("branch", listOf(commit), mutableListOf(commit), true, true, false)
        val branchInfo2 = BranchInfo("branch", listOf(commit), mutableListOf(commit, commit1), true, true, false)
        assertFalse(branchInfo1 == branchInfo2)
    }

    fun testEqualsDifferentObjects() {
        assertNotEquals(branchInfo, commit)
    }

    fun testEqualsSameReference() {
        assertEquals(branchInfo, branchInfo)
    }

    fun testEqualsNullable() {
        assertNotEquals(branchInfo, null)
    }

    fun testHashCode() {
        val branchInfo1 = BranchInfo("branch", listOf(commit), mutableListOf(commit), true, true, false)
        val branchInfo2 = BranchInfo("branch", listOf(commit), mutableListOf(commit), true, true, false)
        assertEquals(branchInfo1.hashCode(), branchInfo2.hashCode())
    }

    fun testAddCommitsToCurrentCommits() {
        branchInfo.addCommitsToCurrentCommits(0, listOf(commit, commit1))
        assertEquals(listOf(commit, commit1), branchInfo.currentCommits)
        verify(listener, times(1)).onCurrentCommitsChange(branchInfo.currentCommits)
    }

    fun testCollapseCommitsSmallerThan7() {
        val commit2 = CommitInfo(TestGitCommitProvider(project).createCommit("bla"), project, mutableListOf())
        branchInfo.setCommits(listOf(commit, commit1, commit2))
        branchInfo.collapseCommits(0, 1)
        assertEquals(3, branchInfo.currentCommits.size)
        assertTrue(branchInfo.currentCommits.filterNot { it.isCollapsed }.size == 3)
    }

    fun testCollapseCommits() {
        val commit2 = CommitInfo(TestGitCommitProvider(project).createCommit("bla"), project, mutableListOf())
        val commit3 = CommitInfo(TestGitCommitProvider(project).createCommit("bla1"), project, mutableListOf())
        val commit4 = CommitInfo(TestGitCommitProvider(project).createCommit("bla2"), project, mutableListOf())
        val commit5 = CommitInfo(TestGitCommitProvider(project).createCommit("bla3"), project, mutableListOf())
        val commit6 = CommitInfo(TestGitCommitProvider(project).createCommit("bla4"), project, mutableListOf())
        val commit7 = CommitInfo(TestGitCommitProvider(project).createCommit("bla5"), project, mutableListOf())
        val commit8 = CommitInfo(TestGitCommitProvider(project).createCommit("bla6"), project, mutableListOf())

        branchInfo.setCommits(listOf(commit, commit1, commit2, commit3, commit4, commit5, commit6, commit7, commit8))
        branchInfo.collapseCommits(2, 4)
        assertEquals(5, branchInfo.currentCommits.size)
        assertThat(branchInfo.currentCommits.filterNot { it.isCollapsed }.size).isEqualTo(3)
    }

    fun testAddSelectedCommits() {
        branchInfo.addSelectedCommits(commit)
        assertEquals(listOf(commit), branchInfo.selectedCommits)
        verify(listener, times(1)).onSelectedCommitChange(branchInfo.selectedCommits)
    }

    fun testClearSelectedCommits() {
        branchInfo.addSelectedCommits(commit)
        commit.isSelected = true
        branchInfo.clearSelectedCommits()
        assertEquals(0, branchInfo.selectedCommits.size)
        assertFalse(commit.isSelected)
        verify(listener, times(2)).onSelectedCommitChange(branchInfo.selectedCommits)
    }

    fun testGetActualSelectedCommitsSize() {
        branchInfo.addSelectedCommits(commit)
        branchInfo.addSelectedCommits(commit1)
        commit.isSquashed = true
        commit.isSelected = true
        assertEquals(1, branchInfo.getActualSelectedCommitsSize())
    }

    fun testIndexOfCommitNotSquashed() {
        branchInfo.setCommits(listOf(commit, commit1))
        assertEquals(0, branchInfo.indexOfCommit(commit))
        assertEquals(1, branchInfo.indexOfCommit(commit1))
    }

    fun testIndexOfCommitSquashed() {
        commit.addChange(SquashCommand(commit, mutableListOf(commit1), "squash"))
        commit.isSquashed = true
        branchInfo.setCommits(listOf(commit1, commit))
        assertEquals(0, branchInfo.indexOfCommit(commit))
    }

    fun testIndexOfCommitFixedUp() {
        commit.addChange(FixupCommand(commit, mutableListOf(commit1)))
        commit.isSquashed = true
        branchInfo.setCommits(listOf(commit1, commit))
        assertEquals(0, branchInfo.indexOfCommit(commit))
    }

    fun testIndexOfCommitSquashedButNoCommand() {
        commit.isSquashed = true
        commit.addChange(DropCommand(commit))
        branchInfo.setCommits(listOf(commit1, commit))
        assertEquals(1, branchInfo.indexOfCommit(commit))
    }
}
