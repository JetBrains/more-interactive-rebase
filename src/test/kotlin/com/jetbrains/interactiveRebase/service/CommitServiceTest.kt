package com.jetbrains.interactiveRebase.service

import com.intellij.mock.MockVirtualFile
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.vcs.log.Hash
import com.intellij.vcs.log.VcsUser
import com.intellij.vcs.log.VcsUserRegistry
import com.intellij.vcs.log.impl.VcsUserImpl
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.exceptions.IRInaccessibleException
import com.jetbrains.interactiveRebase.mockStructs.MockGitRepository
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.threads.CommitInfoThread
import com.jetbrains.interactiveRebase.utils.IRGitUtils
import com.jetbrains.interactiveRebase.utils.consumers.CommitConsumer
import com.jetbrains.interactiveRebase.utils.consumers.GeneralCommitConsumer
import git4idea.GitCommit
import git4idea.history.GitCommitRequirements
import git4idea.repo.GitRepository
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class CommitServiceTest : BasePlatformTestCase() {
    private lateinit var service: CommitService
    private lateinit var thread: CommitInfoThread

    override fun setUp() {
        super.setUp()
        service = project.service<CommitService>()
        thread = CommitInfoThread(project, BranchInfo(mutableListOf(), "|"))
    }

    fun testGeneralConsumerStopsAtCap() {
        val commit1 = createCommit("add tests")
        val commit2 = createCommit("fix tests")
        val consumer = GeneralCommitConsumer()
        consumer.commitConsumptionCap = 1
        consumer.consume(commit1)
        consumer.consume(commit2)
        assertEquals(consumer.commits, listOf(commit1))

        consumer.resetCommits()
        consumer.commitConsumptionCap = 0
        consumer.consume(commit1)
        assertEmpty(consumer.commits)
    }

    fun testGeneralConsumerIgnoresNull() {
        val commit1 = null
        val commit2 = createCommit("fix tests")
        val consumer = GeneralCommitConsumer()
        consumer.commitConsumptionCap = 5
        consumer.consume(commit1)
        consumer.consume(commit2)
        assertEquals(consumer.commits, listOf(commit2))
    }

    fun testDisplayableCommitsCanDisplayWithoutReferenceBranch() {
        val branchName = "feature1"
        val repo: GitRepository = MockGitRepository("current")
        val consumer = GeneralCommitConsumer()
        val commit = createCommit("added tests")

        val utils = mock(IRGitUtils::class.java)
        val serviceWithUtil = CommitService(project, utils)
        serviceWithUtil.referenceBranchName = branchName

        doAnswer {
            consumer.accept(commit)
        }.`when`(utils).getCommitsOfBranch(anyCustom(), anyCustom())

        val res = serviceWithUtil.getDisplayableCommitsOfBranch(branchName, repo, consumer)
        verify(utils).getCommitsOfBranch(anyCustom(), anyCustom())
        assertEquals(res, listOf(commit))
    }

    fun testDisplayableCommitsWithReferenceBranch() {
        val branchName = "feature1"
        val repo: GitRepository = MockGitRepository("current")
        val consumer = GeneralCommitConsumer()
        val commit = createCommit("added tests")

        val utils = mock(IRGitUtils::class.java)
        val serviceWithUtil = CommitService(project, utils)
        serviceWithUtil.referenceBranchName = "main"

        doAnswer {
            consumer.accept(commit)
        }.`when`(utils).getCommitDifferenceBetweenBranches(anyCustom(), anyCustom(), anyCustom(), anyCustom())

        val res = serviceWithUtil.getDisplayableCommitsOfBranch(branchName, repo, consumer)
        verify(utils).getCommitDifferenceBetweenBranches(anyCustom(), anyCustom(), anyCustom(), anyCustom())
        assertEquals(res, listOf(commit))
    }

    fun testGetCommitWorksWithoutNull() {
        val repo: GitRepository = MockGitRepository("current")
        val utils = mock(IRGitUtils::class.java)
        val serviceWithUtil = CommitService(project, utils)

        val commit1 = createCommit("added tests")
        val commit2 = createCommit("fix tests")
        doAnswer {
            repo
        }.`when`(utils).getRepository()

        doAnswer {
            val consumerInside = it.arguments[3] as CommitConsumer
            consumerInside.accept(commit1)
            consumerInside.accept(commit2)
        }.`when`(utils).getCommitDifferenceBetweenBranches(anyCustom(), anyCustom(), anyCustom(), anyCustom())

        val res = serviceWithUtil.getCommits()
        assertEquals(res, listOf(commit1, commit2))
    }

    fun testGetCommitChecksIfRepoIsNull() {
        val utils = mock(IRGitUtils::class.java)
        val serviceWithUtil = CommitService(project, utils)

        doAnswer {
            null
        }.`when`(utils).getRepository()

        assertThrows<IRInaccessibleException> {
            serviceWithUtil.getCommits()
        }
    }

    fun testGetCommitChecksIfBranchIsNull() {
        val repo: GitRepository = MockGitRepository(null)
        val utils = mock(IRGitUtils::class.java)
        val serviceWithUtil = CommitService(project, utils)

        doAnswer {
            repo
        }.`when`(utils).getRepository()

        assertThrows<IRInaccessibleException> {
            serviceWithUtil.getCommits()
        }
    }

    private inline fun <reified T> anyCustom(): T = any(T::class.java)

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
}
