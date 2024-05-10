package com.jetbrains.interactiveRebase.service

import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vcs.VcsException
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.exceptions.IRInaccessibleException
import com.jetbrains.interactiveRebase.services.BranchService
import com.jetbrains.interactiveRebase.utils.IRGitUtils
import git4idea.commands.GitCommandResult
import org.assertj.core.api.Assertions
import org.mockito.Mockito

class BranchServiceTest : BasePlatformTestCase() {
    private lateinit var controlledService: BranchService
    private lateinit var service: BranchService
    private lateinit var utils: IRGitUtils

    override fun setUp() {
        super.setUp()
        utils = Mockito.mock(IRGitUtils::class.java)
        controlledService = BranchService(project, utils)
        service = BranchService(project)
    }

    fun testDefaultRefBranchNameChanges() {
        val actualPrimaryBranch = "master"

        val commandResult = GitCommandResult(false, 0, listOf(), listOf(actualPrimaryBranch))

        Mockito.doAnswer {
            project.guessProjectDir()
        }.`when`(utils).getRoot()

        Mockito.doAnswer {
            commandResult
        }.`when`(utils).runCommand(anyCustom())

        val result = controlledService.getDefaultReferenceBranchName()
        assertEquals(result, actualPrimaryBranch)
    }

    fun testRefBranchNameFormatsCombination() {
        val actualPrimaryBranch = "master"
        val commandResult = GitCommandResult(false, 0, listOf(), listOf("   *$actualPrimaryBranch  "))

        Mockito.doAnswer {
            project.guessProjectDir()
        }.`when`(utils).getRoot()

        Mockito.doAnswer {
            commandResult
        }.`when`(utils).runCommand(anyCustom())

        val result = controlledService.getDefaultReferenceBranchName()
        assertEquals(result, actualPrimaryBranch)
    }

    fun testRefBranchNameFormatsSpace() {
        val actualPrimaryBranch = "master"
        val commandResult = GitCommandResult(false, 0, listOf(), listOf("   $actualPrimaryBranch  "))

        Mockito.doAnswer {
            project.guessProjectDir()
        }.`when`(utils).getRoot()

        Mockito.doAnswer {
            commandResult
        }.`when`(utils).runCommand(anyCustom())

        val result = controlledService.getDefaultReferenceBranchName()
        assertEquals(result, actualPrimaryBranch)
    }

    fun testRefBranchNameFormatsAsterisk() {
        val actualPrimaryBranch = "master"
        val commandResult = GitCommandResult(false, 0, listOf(), listOf("*$actualPrimaryBranch"))

        Mockito.doAnswer {
            project.guessProjectDir()
        }.`when`(utils).getRoot()

        Mockito.doAnswer {
            commandResult
        }.`when`(utils).runCommand(anyCustom())

        val result = controlledService.getDefaultReferenceBranchName()
        assertEquals(result, actualPrimaryBranch)
    }

    fun testRefBranchChecksIfRootIsNull() {
        Mockito.doAnswer {
            null
        }.`when`(utils).getRoot()

        val exception =
            org.junit.jupiter.api.assertThrows<IRInaccessibleException> {
                controlledService.getDefaultReferenceBranchName()
            }
        assertEquals(exception.message, "Project root cannot be found")
    }

    fun testisMergedBranchSplits() {
        val b1 = "1-test-branch"
        val b2 = "another_branch"
        val b3 = "55555-THESE-are-already-merged"
        val branches = "$b1\n$b2\n$b3"

        val commandResult = GitCommandResult(false, 0, listOf(), listOf(branches))
        Mockito.doAnswer {
            project.guessProjectDir()
        }.`when`(utils).getRoot()
        Mockito.doAnswer {
            commandResult
        }.`when`(utils).runCommand(anyCustom())

        assertTrue(controlledService.isBranchMerged(b1))
        assertTrue(controlledService.isBranchMerged(b2))
        assertTrue(controlledService.isBranchMerged(b3))
        assertFalse(controlledService.isBranchMerged("33333-not-merged-branch"))
    }

    fun testisMergedBranchReformats() {
        val b1 = "1-test-branch"
        val b2 = "another_branch"
        val b3 = "55555-THESE-are-already-merged"
        val branches = "  $b1\n*$b2\n $b3 "

        val commandResult = GitCommandResult(false, 0, listOf(), listOf(branches))
        Mockito.doAnswer {
            project.guessProjectDir()
        }.`when`(utils).getRoot()
        Mockito.doAnswer {
            commandResult
        }.`when`(utils).runCommand(anyCustom())

        assertTrue(controlledService.isBranchMerged(b1))
        assertTrue(controlledService.isBranchMerged(b2))
        assertTrue(controlledService.isBranchMerged(b3))
        assertFalse(controlledService.isBranchMerged("33333-not-merged-branch"))
    }

    fun testisMergedBranchHandlesStartError() {
        val b1 = "1-test-branch"
        val b2 = "another_branch"
        val b3 = "55555-THESE-are-already-merged"
        val branches = "  $b1\n*$b2\n $b3 "

        val commandResult = GitCommandResult(true, 0, listOf(), listOf(branches))
        Mockito.doAnswer {
            project.guessProjectDir()
        }.`when`(utils).getRoot()
        Mockito.doAnswer {
            commandResult
        }.`when`(utils).runCommand(anyCustom())

        org.junit.jupiter.api.assertThrows<VcsException> {
            controlledService.isBranchMerged(b1)
        }
    }

    fun testisMergedBranchHandlesError() {
        val b1 = "1-test-branch"
        val b2 = "another_branch"
        val b3 = "55555-THESE-are-already-merged"
        val branches = "  $b1\n*$b2\n $b3 "

        val commandResult = GitCommandResult(false, 88, listOf("fatal error..."), listOf(branches))
        Mockito.doAnswer {
            project.guessProjectDir()
        }.`when`(utils).getRoot()
        Mockito.doAnswer {
            commandResult
        }.`when`(utils).runCommand(anyCustom())

        val exception =
            org.junit.jupiter.api.assertThrows<VcsException> {
                controlledService.isBranchMerged(b1)
            }
        Assertions.assertThat(exception.message).isEqualTo("fatal error...")
    }

    private inline fun <reified T> anyCustom(): T = Mockito.any(T::class.java)
}
