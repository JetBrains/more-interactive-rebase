package com.jetbrains.interactiveRebase.visuals

import com.intellij.mock.MockVirtualFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.vcs.log.Hash
import com.intellij.vcs.log.VcsUser
import com.intellij.vcs.log.VcsUserRegistry
import com.intellij.vcs.log.impl.VcsUserImpl
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import git4idea.GitCommit
import git4idea.history.GitCommitRequirements

class CommitInfoPanelTest : BasePlatformTestCase() {
    private lateinit var branchInfo: BranchInfo
    private lateinit var commit1: CommitInfo
    private lateinit var commit2: CommitInfo
    private lateinit var commit3: CommitInfo

    override fun setUp() {
        super.setUp()
        commit1 = CommitInfo(createCommit("my commit"), project, mutableListOf())
        commit2 = CommitInfo(createCommit("my other commit"), project, mutableListOf())
        commit3 = CommitInfo(createCommit("my last commit"), project, mutableListOf())

        branchInfo = BranchInfo(initialCommits = listOf(commit1, commit2, commit3))
    }

    fun testNoCommitsSelected() {
        var changes = mutableListOf<Change>()
        val thread = TestThread(project, listOf(), changes)
        thread.start()
        thread.join()
        assertTrue(changes.isEmpty())
    }

    fun testCommitsSelected() {
        var changes = mutableListOf<Change>()
        val thread = TestThread(project, listOf(commit1.commit), changes)
        thread.start()
        thread.join()
        assertTrue(changes.isEmpty())
    }

    private fun createCommit(subject: String): GitCommit {
        val author = MockVcsUserRegistry().users.first()
        val hash = MockHash()
        val root = MockVirtualFile("mock name")
        val message = "example long commit message"
        val commitRequirements = GitCommitRequirements()
        return GitCommit(project, hash, listOf(), 1000L, root, subject, author, message, author, 1000L, listOf(), commitRequirements)
    }

    private class MockVcsUserRegistry : VcsUserRegistry {
        override fun getUsers(): MutableSet<VcsUser> {
            return mutableSetOf(
                createUser("abc", "abc@goodmail.com"),
                createUser("aaa", "aaa@badmail.com"),
            )
        }

        override fun createUser(
            name: String,
            email: String,
        ): VcsUser {
            return VcsUserImpl(name, email)
        }
    }

    private class MockHash : Hash {
        override fun asString(): String {
            return "exampleHash"
        }

        override fun toShortString(): String {
            return "exampleShortHash"
        }
    }

    private class TestThread(
        private val project: Project,
        private val commits: List<GitCommit>,
        private var changes: MutableList<Change>,
    ) : Thread() {
        override fun run() {
            val commitInfoPanel = CommitInfoPanel(project)
            changes.addAll(commitInfoPanel.loadChanges(commits))
        }
    }
}
