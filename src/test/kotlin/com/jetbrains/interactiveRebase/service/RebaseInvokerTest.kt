package com.jetbrains.interactiveRebase.service

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.FixupCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.PickCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import git4idea.GitCommit
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito
import org.mockito.Mockito.mock

class RebaseInvokerTest : BasePlatformTestCase() {
    private lateinit var commit1: CommitInfo
    private lateinit var commit2: CommitInfo
    private lateinit var commit3: CommitInfo
    private lateinit var commit4: CommitInfo
    private lateinit var commit5: CommitInfo

    override fun setUp() {
        super.setUp()
        commit1 = CommitInfo(mock(GitCommit::class.java), project)
        commit2 = CommitInfo(mock(GitCommit::class.java), project)
        commit3 = CommitInfo(mock(GitCommit::class.java), project)
        commit4 = CommitInfo(mock(GitCommit::class.java), project)
        commit5 = CommitInfo(mock(GitCommit::class.java), project)

        // setup for squashed commits
        val squashCommand = SquashCommand(commit4, mutableListOf(commit1, commit3), "lol")
        commit4.addChange(squashCommand)
    }

    fun testAddCommand() {
        val rebaseInvoker = RebaseInvoker(project)
        val dropCommand = Mockito.mock(DropCommand::class.java)
        rebaseInvoker.addCommand(dropCommand)
        assertTrue(rebaseInvoker.commands.size == 1)
    }

    fun testRemoveCommand() {
        val rebaseInvoker = RebaseInvoker(project)
        val pickCommand = Mockito.mock(PickCommand::class.java)
        rebaseInvoker.commands = mutableListOf(pickCommand)
        rebaseInvoker.removeCommand(pickCommand)
        assertTrue(rebaseInvoker.commands.isEmpty())
    }

    fun testExpandListSquash() {
        val rebaseInvoker = RebaseInvoker(project)
        rebaseInvoker.branchInfo.currentCommits = mutableListOf(commit2, commit4, commit5)
        rebaseInvoker.expandCurrentCommits()
        assertThat(rebaseInvoker.branchInfo.currentCommits.size).isEqualTo(5)
        assertThat(rebaseInvoker.branchInfo.currentCommits).isEqualTo(mutableListOf(commit2, commit1, commit3, commit4, commit5))
    }

    fun testExpandListFixupAndSquash() {
        val rebaseInvoker = RebaseInvoker(project)
        commit5.addChange(FixupCommand(commit5, mutableListOf(commit2)))
        rebaseInvoker.branchInfo.currentCommits = mutableListOf(commit4, commit5)
        rebaseInvoker.expandCurrentCommits()
        assertThat(rebaseInvoker.branchInfo.currentCommits.size).isEqualTo(5)
        assertThat(rebaseInvoker.branchInfo.currentCommits).isEqualTo(mutableListOf(commit1, commit3, commit4, commit2, commit5))
    }
}
