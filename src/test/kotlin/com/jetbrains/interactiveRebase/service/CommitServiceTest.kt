package com.jetbrains.interactiveRebase.service

import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.exceptions.IRInaccessibleException
import com.jetbrains.interactiveRebase.mockStructs.MockGitRepository
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.BranchService
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.utils.consumers.CommitConsumer
import com.jetbrains.interactiveRebase.utils.consumers.GeneralCommitConsumer
import com.jetbrains.interactiveRebase.utils.gitUtils.IRGitUtils
import git4idea.GitCommit
import git4idea.commands.GitCommandResult
import git4idea.repo.GitRepository
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify

class CommitServiceTest : BasePlatformTestCase() {
    private lateinit var commitService: CommitService
    private lateinit var controlledCommitService: CommitService
    private lateinit var utils: IRGitUtils
    private lateinit var branchService: BranchService
    private lateinit var commitProvider: TestGitCommitProvider

    override fun setUp() {
        super.setUp()
        commitService = project.service<CommitService>()
        commitProvider = TestGitCommitProvider(project)
        branchService = project.service<BranchService>()
        utils = mock(IRGitUtils::class.java)
        val branchService = BranchService(project, utils)

        doAnswer {
            project.guessProjectDir()
        }.`when`(utils).getRoot()
        doAnswer {
            GitCommandResult(false, 0, listOf(), listOf("master"))
        }.`when`(utils).runCommand(anyCustom())
        controlledCommitService = CommitService(project, utils, branchService)
    }

    fun testGeneralConsumerStopsAtCap() {
        val commit1 = commitProvider.createCommit("add tests")
        val commit2 = commitProvider.createCommit("fix tests")
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
        val commit2 = commitProvider.createCommit("fix tests")
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
        val commit = commitProvider.createCommit("added tests")

        controlledCommitService.referenceBranchName = branchName

        doAnswer {
            consumer.accept(commit)
        }.`when`(utils).getCommitsOfBranch(anyCustom(), anyCustom(), anyCustom())

        val res = controlledCommitService.getDisplayableCommitsOfBranch(branchName, repo, consumer)
        verify(utils).getCommitsOfBranch(anyCustom(), anyCustom(), anyCustom())
        assertEquals(res, listOf(commit))
    }

    fun testDisplayableCommitsWithReferenceBranch() {
        val branchName = "feature1"
        val repo: GitRepository = MockGitRepository("current")
        val consumer = GeneralCommitConsumer()
        val commit = commitProvider.createCommit("added tests")
        controlledCommitService.referenceBranchName = "main"

        doAnswer {
            consumer.accept(commit)
        }.`when`(utils).getCommitDifferenceBetweenBranches(anyCustom(), anyCustom(), anyCustom(), anyCustom())

        doAnswer {
            GitCommandResult(false, 0, listOf(), listOf("master"))
        }.`when`(utils).runCommand(anyCustom())

        val res = controlledCommitService.getDisplayableCommitsOfBranch(branchName, repo, consumer)
        verify(utils).getCommitDifferenceBetweenBranches(anyCustom(), anyCustom(), anyCustom(), anyCustom())
        assertEquals(res, listOf(commit))
    }

    fun testGetCommitWorksWithoutNull() {
        val repo: GitRepository = MockGitRepository("current")

        val commit1 = commitProvider.createCommit("added tests")
        val commit2 = commitProvider.createCommit("fix tests")
        doAnswer {
            repo
        }.`when`(utils).getRepository()

        doAnswer {
            val consumerInside = it.arguments[3] as CommitConsumer
            consumerInside.accept(commit1)
            consumerInside.accept(commit2)
        }.`when`(utils).getCommitDifferenceBetweenBranches(anyCustom(), anyCustom(), anyCustom(), anyCustom())

        val res = controlledCommitService.getCommits("current")
        assertEquals(res, listOf(commit1, commit2))
    }

    fun testGetCommitWorksWithNoDefaultRefBranch() {
        val repo: GitRepository = MockGitRepository("current")

        val commit1 = commitProvider.createCommit("added tests")
        val commit2 = commitProvider.createCommit("fix tests")
        doAnswer {
            repo
        }.`when`(utils).getRepository()

        doAnswer {
            GitCommandResult(false, 0, listOf(), listOf(""))
        }.`when`(utils).runCommand(anyCustom())

        doAnswer {
            val consumerInside = it.arguments[1] as CommitConsumer
            consumerInside.accept(commit1)
            consumerInside.accept(commit2)
        }.`when`(utils).getCommitsOfBranch(anyCustom(), anyCustom(), anyCustom())

        val res = controlledCommitService.getCommits("current")

        assertEquals(res, listOf(commit1, commit2))
        assertEquals("current", controlledCommitService.referenceBranchName)
    }

    fun testMergedBranchHandlingConsidersEmptyDiff() {
        val repo: GitRepository = MockGitRepository("current")
        val cons = GeneralCommitConsumer()
        cons.consume(commitProvider.createCommit("fix tests"))
        controlledCommitService.handleMergedBranch(cons, "branch", repo)
        verify(utils, never()).getCommitsOfBranch(repo, cons, "branch")
    }

    fun testMergedBranchHandlingConsidersMerged() {
        val repo: GitRepository = MockGitRepository("current")
        val cons = GeneralCommitConsumer()
        doAnswer {
            GitCommandResult(false, 0, listOf(), listOf("merged"))
        }.`when`(utils).runCommand(anyCustom())
        controlledCommitService.handleMergedBranch(cons, "merged", repo)
        verify(utils).getCommitsOfBranch(repo, cons, "merged")
    }

    fun testGetCommitInfoForBranch() {
        val repo: GitRepository = MockGitRepository("current")

        val commit1 = commitProvider.createCommit("added tests")
        val commit2 = commitProvider.createCommit("fix tests")
        doAnswer {
            repo
        }.`when`(utils).getRepository()

        doAnswer {
            val consumerInside = it.arguments[3] as CommitConsumer
            consumerInside.accept(commit1)
            consumerInside.accept(commit2)
        }.`when`(utils).getCommitDifferenceBetweenBranches(anyCustom(), anyCustom(), anyCustom(), anyCustom())

        val res = controlledCommitService.getCommitInfoForBranch(listOf(commit1, commit2))
        assertEquals(res.size, 2)
        assertEquals(res[0].commit, commit1)
        assertEquals(res[1].commit, commit2)
    }

    fun testTurnHashToCommit() {
        val commit1: GitCommit = commitProvider.createCommit("added tests")
        doAnswer {
            MockGitRepository("current-branch")
        }.`when`(utils).getRepository()
        doAnswer {
            val consumerInside = it.arguments[2] as CommitConsumer
            consumerInside.accept(commit1)
        }.`when`(utils).collectACommit(anyCustom(), anyCustom(), anyCustom())
        val res = controlledCommitService.turnHashToCommit(commit1.id.toString())
        assertEquals(res, commit1)
    }

    fun testTurnHashToCommitCheckNoCommits() {
        val commit1: GitCommit = commitProvider.createCommit("added tests")
        doAnswer {
            MockGitRepository("current-branch")
        }.`when`(utils).getRepository()
        doNothing().`when`(utils).collectACommit(anyCustom(), anyCustom(), anyCustom())
        assertThrows<IRInaccessibleException> { controlledCommitService.turnHashToCommit(commit1.id.toString()) }
    }

    fun testGetBranchName() {
        doAnswer {
            MockGitRepository("current-branch")
        }.`when`(utils).getRepository()
        assertEquals(controlledCommitService.getBranchName(), "current-branch")
    }

    private inline fun <reified T> anyCustom(): T = any(T::class.java)
}
