package com.jetbrains.interactiveRebase.service

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.FixupCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.PickCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import git4ideaClasses.GitRebaseEntryGeneratedUsingLog
import git4ideaClasses.IRGitModel
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito

class RebaseInvokerTest : BasePlatformTestCase() {
    private lateinit var commit1: CommitInfo
    private lateinit var commit2: CommitInfo
    private lateinit var commit3: CommitInfo
    private lateinit var commit4: CommitInfo
    private lateinit var commit5: CommitInfo
    private lateinit var commitProvider: TestGitCommitProvider

    override fun setUp() {
        super.setUp()
        commitProvider = TestGitCommitProvider(project)
        commit1 = CommitInfo(commitProvider.createCommit("commit1"), project)
        commit2 = CommitInfo(commitProvider.createCommit("commit2"), project)
        commit3 = CommitInfo(commitProvider.createCommit("commit3"), project)
        commit4 = CommitInfo(commitProvider.createCommit("commit4"), project)
        commit5 = CommitInfo(commitProvider.createCommit("commit5"), project)
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
        // setup for squashed commits
        val squashCommand = SquashCommand(commit4, mutableListOf(commit1, commit3), "lol")
        commit4.addChange(squashCommand)
        val rebaseInvoker = RebaseInvoker(project)
        rebaseInvoker.branchInfo.currentCommits = mutableListOf(commit2, commit4, commit5)
        rebaseInvoker.expandCurrentCommitsForSquashed()
        assertThat(rebaseInvoker.branchInfo.currentCommits.size).isEqualTo(5)
        assertThat(rebaseInvoker.branchInfo.currentCommits).isEqualTo(mutableListOf(commit2, commit1, commit3, commit4, commit5))
    }

    fun testExpandListFixupAndSquash() {
        // setup for squashed commits
        val squashCommand = SquashCommand(commit4, mutableListOf(commit1, commit3), "lol")
        commit4.addChange(squashCommand)
        val rebaseInvoker = RebaseInvoker(project)
        commit5.addChange(FixupCommand(commit5, mutableListOf(commit2)))
        rebaseInvoker.branchInfo.currentCommits = mutableListOf(commit4, commit5)
        rebaseInvoker.expandCurrentCommitsForSquashed()
        assertThat(rebaseInvoker.branchInfo.currentCommits.size).isEqualTo(5)
        assertThat(rebaseInvoker.branchInfo.currentCommits).isEqualTo(
            mutableListOf(
                commit1,
                commit3,
                commit4,
                commit2,
                commit5,
            ),
        )
    }

    fun testCreateModel() {
        val rebaseInvoker = RebaseInvoker(project)
        rebaseInvoker.branchInfo.currentCommits = mutableListOf(commit2, commit4, commit5)
        rebaseInvoker.createModel()
        assertThat(rebaseInvoker.model.elements[0].index).isEqualTo(0)
        assertThat(rebaseInvoker.model.elements[0].type).isEqualTo(IRGitModel.Type.NonUnite.KeepCommit.Pick)
        assertThat(rebaseInvoker.model.elements[0].entry).isEqualTo(GitRebaseEntryGeneratedUsingLog(commit5.commit))
        assertThat(rebaseInvoker.model.elements[1].index).isEqualTo(1)
        assertThat(rebaseInvoker.model.elements[1].type).isEqualTo(IRGitModel.Type.NonUnite.KeepCommit.Pick)
        assertThat(rebaseInvoker.model.elements[1].entry).isEqualTo(GitRebaseEntryGeneratedUsingLog(commit4.commit))
        assertThat(rebaseInvoker.model.elements[2].index).isEqualTo(2)
        assertThat(rebaseInvoker.model.elements[2].type).isEqualTo(IRGitModel.Type.NonUnite.KeepCommit.Pick)
        assertThat(rebaseInvoker.model.elements[2].entry).isEqualTo(GitRebaseEntryGeneratedUsingLog(commit2.commit))
    }
}
