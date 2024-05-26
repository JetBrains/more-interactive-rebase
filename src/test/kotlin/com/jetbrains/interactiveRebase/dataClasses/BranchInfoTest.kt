package com.jetbrains.interactiveRebase.dataClasses

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations.openMocks

class BranchInfoTest : BasePlatformTestCase() {
    private lateinit var branchInfo: BranchInfo
    private lateinit var listener: BranchInfo.Listener
    private lateinit var commit: CommitInfo

    override fun setUp() {
        super.setUp()
        branchInfo = BranchInfo()
        val commitProvider = TestGitCommitProvider(project)
        commit = CommitInfo(commitProvider.createCommit("tests"), project, mutableListOf())
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
}
