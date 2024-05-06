package com.jetbrains.interactiveRebase

import com.intellij.dvcs.repo.Repository
import com.intellij.dvcs.repo.RepositoryImpl
import com.intellij.mock.MockVirtualFile
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.AbstractVcs
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.vcs.log.Hash
import com.intellij.vcs.log.VcsUser
import com.intellij.vcs.log.VcsUserRegistry
import com.intellij.vcs.log.impl.VcsUserImpl
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.threads.CommitInfoThread
import git4idea.GitCommit
import git4idea.GitLocalBranch
import git4idea.GitVcs
import git4idea.branch.GitBranchesCollection
import git4idea.history.GitCommitRequirements
import git4idea.ignore.GitRepositoryIgnoredFilesHolder
import git4idea.repo.*
import git4idea.status.GitStagingAreaHolder
import org.mockito.Mockito.*


class CommitServiceTest : BasePlatformTestCase() {
    private lateinit var service : CommitService
    private lateinit var thread : CommitInfoThread
    override fun setUp() {
        super.setUp()
        service = project.service<CommitService>()
        thread = CommitInfoThread(project)
    }

    fun testGeneralConsumerStopsAtCap() {
        val commit1 = createCommit("add tests")
        val commit2 = createCommit("fix tests")
        val consumer = CommitService.GeneralCommitConsumer()
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
        val consumer = CommitService.GeneralCommitConsumer()
        consumer.commitConsumptionCap = 5
        consumer.consume(commit1)
        consumer.consume(commit2)
        assertEquals(consumer.commits, listOf(commit2))
    }

//    fun testDisplayableCommitsCanDisplayNoReferenceBranch() {
//        val branchName = "feature1"
//        val repo = MockRepositoryImpl(null, null, null, null)
//        service.setReferenceBranch(branchName)
//        service.getDisplayableCommitsOfBranch(branchName,)
//    }

//    fun generalConsumerStopsAtCap() {
//        val repo = mock(GitRepository::class.java)
//        val commit1 = mock(GitCommit::class.java)
//        val consumer = CommitService.GeneralCommitConsumer()
//        consumer.commitConsumptionCap = 1
//
//
//
//        assertEquals(consumer.commits, )
//    }
//



//    fun testRename() {
//        myFixture.testRename("foo.xml", "foo_after.xml", "a2")
//    }


//    fun testProjectService() {
//        val projectService = project.service<MyProjectService>()
//
//        Assert.assertNotSame(projectService.getRandomNumber(), projectService.getRandomNumber())
//    }

    override fun getTestDataPath() = "src/test/testData/rename"

    private fun createCommit(subject : String) : GitCommit {
        val author = MockVcsUserRegistry().users.first()
        val hash = MockHash()
        val root = MockVirtualFile("mock name")
        val message = "example long commit message"
        val commitRequirements = GitCommitRequirements()
        return GitCommit(project, hash, listOf(), 1000L, root, subject, author, message, author, 1000L, listOf(), commitRequirements )
    }


    private class MockVcsUserRegistry : VcsUserRegistry {
        override fun getUsers(): MutableSet<VcsUser> {
            return mutableSetOf(createUser("abc", "abc@goodmail.com"),
                createUser("aaa", "aaa@badmail.com"));
        }

        override fun createUser(name: String, email: String): VcsUser {
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

//    private class MockRepositoryImpl : RepositoryImpl(null, null, null) {
//        override fun getState(): Repository.State {
//            throw UnsupportedOperationException()
//        }
//
//        override fun getCurrentBranchName(): String? {
//            throw UnsupportedOperationException()
//        }
//
//        override fun getVcs(): AbstractVcs {
//            throw UnsupportedOperationException()
//        }
//
//        override fun getCurrentRevision(): String? {
//            throw UnsupportedOperationException()
//        }
//
//        override fun update() {
//            throw UnsupportedOperationException()
//        }
//
//        override fun toLogString(): String {
//            throw UnsupportedOperationException()
//        }
//
//    }

}
