package com.jetbrains.interactiveRebase.mockStructs

import com.intellij.mock.MockVirtualFile
import com.intellij.openapi.project.Project
import com.intellij.vcs.log.Hash
import com.intellij.vcs.log.VcsUser
import com.intellij.vcs.log.VcsUserRegistry
import com.intellij.vcs.log.impl.VcsUserImpl
import git4idea.GitCommit
import git4idea.history.GitCommitRequirements

class TestGitCommitProvider(private val project: Project) {
    fun createCommit(subject: String): GitCommit {
        val author = MockVcsUserRegistry().users.first()
        val hash = MockHash(subject)
        val root = MockVirtualFile("mock name")
        val message = "example long commit message"
        val commitRequirements = GitCommitRequirements()
        return GitCommit(
            project, hash, listOf(), 1000L, root, subject, author,
            message, author, 1000L, listOf(), commitRequirements,
        )
    }

    fun createCommitWithParent(
        thisCommit: String,
        vararg parents: String,
    ): GitCommit {
        val author = MockVcsUserRegistry().users.first()
        val hash = MockHash(thisCommit)
        val root = MockVirtualFile("mock name")
        val message = "example long commit message"
        val commitRequirements = GitCommitRequirements()
        val parentsList = parents.map { MockHash(it) }
        return GitCommit(
            project, hash, parentsList, 1000L, root, thisCommit, author,
            message, author, 1000L, listOf(), commitRequirements,
        )
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

    class MockHash(private val string: String) : Hash {
        override fun asString(): String {
            return string
        }

        override fun toShortString(): String {
            return string
        }

        override fun toString(): String {
            return "MockHash(string='$string')"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MockHash

            return string == other.string
        }

        override fun hashCode(): Int {
            return string.hashCode()
        }
    }
}
