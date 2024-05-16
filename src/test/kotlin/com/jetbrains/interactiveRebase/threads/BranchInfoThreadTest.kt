package com.jetbrains.interactiveRebase.threads

import com.intellij.mock.MockVirtualFile
import com.intellij.openapi.components.service
import com.intellij.openapi.project.guessProjectDir
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.vcs.log.Hash
import com.intellij.vcs.log.VcsUser
import com.intellij.vcs.log.VcsUserRegistry
import com.intellij.vcs.log.impl.VcsUserImpl
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.mockStructs.MockGitRepository
import com.jetbrains.interactiveRebase.services.BranchService
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.services.ComponentService
import com.jetbrains.interactiveRebase.utils.consumers.CommitConsumer
import com.jetbrains.interactiveRebase.utils.gitUtils.IRGitUtils
import git4idea.GitCommit
import git4idea.commands.GitCommandResult
import git4idea.history.GitCommitRequirements
import git4idea.repo.GitRepository
import org.mockito.Mockito.any
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock

class BranchInfoThreadTest : BasePlatformTestCase() {
    private lateinit var service: CommitService
    private lateinit var commitService: CommitService
    private lateinit var utils: IRGitUtils
    private lateinit var branchService: BranchService
    private lateinit var componentService: ComponentService

    override fun setUp() {
        super.setUp()
        service = project.service<CommitService>()
        utils = mock(IRGitUtils::class.java)
        branchService = BranchService(project, utils)
        commitService = CommitService(project, utils, branchService)
        componentService = project.service<ComponentService>()

        doAnswer {
            project.guessProjectDir()
        }.`when`(utils).getRoot()
        doAnswer {
            GitCommandResult(false, 0, listOf(), listOf("master"))
        }.`when`(utils).runCommand(anyCustom())
    }

    fun testUpdateBranchInfoEmptyName() {
        val repo = MockGitRepository("my branch")

        val commit1 = createCommit("added tests")
        val commit2 = createCommit("fix tests")

        val testBranchInfo = BranchInfo()
        doAnswer {
            repo
        }.`when`(utils).getRepository()

        val thread = BranchInfoThread(project, testBranchInfo, commitService, componentService)

        doAnswer {
            val consumerInside = it.arguments[3] as CommitConsumer
            consumerInside.accept(commit1)
            consumerInside.accept(commit2)
        }.`when`(utils).getCommitDifferenceBetweenBranches(anyCustom(), anyCustom(), anyCustom(), anyCustom())

        thread.start()
        thread.join()
        assertEquals("my branch", testBranchInfo.name)
        assertEquals(commit1, testBranchInfo.commits[0].commit)
        assertEquals(commit2, testBranchInfo.commits[1].commit)
        assertEquals(testBranchInfo.selectedCommits.size, 0)
    }

    fun testUpdateBranchInfoDifferentBranch() {
        val repo: GitRepository = MockGitRepository("my branch")

        val commit1 = createCommit("added tests")
        val commit2 = createCommit("fix tests")

        val oldCommit = CommitInfo(createCommit("old commit"), project, null)

        val testBranchInfo = BranchInfo("bug-fix", commits = listOf(oldCommit), false)
        testBranchInfo.selectedCommits.add(oldCommit)

        val thread = BranchInfoThread(project, testBranchInfo, commitService, componentService)

        doAnswer {
            repo
        }.`when`(utils).getRepository()

        doAnswer {
            val consumerInside = it.arguments[3] as CommitConsumer
            consumerInside.accept(commit1)
            consumerInside.accept(commit2)
        }.`when`(utils).getCommitDifferenceBetweenBranches(anyCustom(), anyCustom(), anyCustom(), anyCustom())

        thread.start()
        thread.join()
        assertEquals("my branch", testBranchInfo.name)
        assertEquals(commit1, testBranchInfo.commits[0].commit)
        assertEquals(commit2, testBranchInfo.commits[1].commit)
        assertEquals(0, testBranchInfo.selectedCommits.size)
    }

    fun testUpdateBranchInfoNoUpdate() {
        val repo: GitRepository = MockGitRepository("my branch")
        val utils = mock(IRGitUtils::class.java)
        val commitService = CommitService(project, utils, project.service<BranchService>())
        val componentService = project.service<ComponentService>()

        val commit1 = createCommit("added tests")
        val commit2 = createCommit("fix tests")

        val commitInfo1 = CommitInfo(commit1, project, null)
        val commitInfo2 = CommitInfo(commit2, project, null)

        val testBranchInfo = BranchInfo("my branch", commits = listOf(commitInfo1, commitInfo2), false)
        testBranchInfo.selectedCommits.add(commitInfo1)

        val thread = BranchInfoThread(project, testBranchInfo, commitService, componentService)

        doAnswer {
            repo
        }.`when`(utils).getRepository()

        doAnswer {
            val consumerInside = it.arguments[3] as CommitConsumer
            consumerInside.accept(commit1)
            consumerInside.accept(commit2)
        }.`when`(utils).getCommitDifferenceBetweenBranches(anyCustom(), anyCustom(), anyCustom(), anyCustom())

        thread.start()
        thread.join()

        assertEquals("my branch", testBranchInfo.name)
        assertEquals(commit1, testBranchInfo.commits[0].commit)
        assertEquals(commit2, testBranchInfo.commits[1].commit)
        assertEquals(listOf(commitInfo1), testBranchInfo.selectedCommits)
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
