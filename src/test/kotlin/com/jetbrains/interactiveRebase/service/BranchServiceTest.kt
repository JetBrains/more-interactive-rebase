package com.jetbrains.interactiveRebase.service

import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vcs.VcsException
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.exceptions.IRInaccessibleException
import com.jetbrains.interactiveRebase.mockStructs.MockGitRepository
import com.jetbrains.interactiveRebase.services.BranchService
import com.jetbrains.interactiveRebase.utils.gitUtils.IRGitUtils
import git4idea.commands.GitCommandResult
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.assertThrows
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
            assertThrows<IRInaccessibleException> {
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

        assertThrows<VcsException> {
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
            assertThrows<VcsException> {
                controlledService.isBranchMerged(b1)
            }
        Assertions.assertThat(exception.message).isEqualTo("fatal error...")
    }

    fun testValidateRefBranchChecksEmpty() {
        assertNull(service.validateReferenceBranchOutput(""))
    }

    fun testRefBranchValidationChecksBothMainMaster() {
        assertNull(service.validateReferenceBranchOutput("master\\nmain"))
    }

    fun testGetBranches() {
        val b1 = "1-test-branch"
        val b2 = "another_branch"
        val b3 = "55555-THESE-are-already-merged"
        val b4 = "sdf"
        val branches = "  $b1\n*$b2\n $b3 \n $b4\n"

        val commandResult = GitCommandResult(false, 0, listOf(), listOf(branches))
        Mockito.doAnswer {
            project.guessProjectDir()
        }.`when`(utils).getRoot()
        Mockito.doAnswer {
            commandResult
        }.`when`(utils).runCommand(anyCustom())
        val branchList = controlledService.getBranches()
        assertEquals(branchList, listOf(b1, b2, b3, b4))
    }

    fun testGetBranchesHandlesOne() {
        val branches = "adadada\n"
        val commandResult = GitCommandResult(false, 0, listOf(), listOf(branches))
        Mockito.doAnswer {
            project.guessProjectDir()
        }.`when`(utils).getRoot()
        Mockito.doAnswer {
            commandResult
        }.`when`(utils).runCommand(anyCustom())
        val branchList = controlledService.getBranches()
        assertEquals(branchList, listOf("adadada"))
    }

    fun testGetBranchesHandlesEmpty() {
        val branches = "\n"
        val commandResult = GitCommandResult(false, 0, listOf(), listOf(branches))
        Mockito.doAnswer {
            project.guessProjectDir()
        }.`when`(utils).getRoot()
        Mockito.doAnswer {
            commandResult
        }.`when`(utils).runCommand(anyCustom())
        val branchList = controlledService.getBranches()
        assertEquals(branchList, listOf<String>())
    }

    fun testGetBranchesExceptCheckedOut() {
        val b1 = "1-test-branch"
        val b2 = "another_branch"
        val b3 = "55555-THESE-are-already-merged"
        val b4 = "current"
        val branches = "  $b1\n*$b2\n $b3 \n $b4\n"

        val commandResult = GitCommandResult(false, 0, listOf(), listOf(branches))
        Mockito.doAnswer {
            project.guessProjectDir()
        }.`when`(utils).getRoot()
        Mockito.doAnswer {
            MockGitRepository("current")
        }.`when`(utils).getRepository()

        Mockito.doAnswer {
            commandResult
        }.`when`(utils).runCommand(anyCustom())

        val list = controlledService.getBranchesExceptCheckedOut()
        assertFalse(list.contains("current"))
        assertEquals(3, list.size)
    }

    fun testGetBranchesExceptCheckedOutDoesntInclude() {
        val b1 = "1-test-branch"
        val b2 = "another_branch"
        val b3 = "55555-THESE-are-already-merged"
        val b4 = "current"
        val branches = "  $b1\n*$b2\n $b3 \n $b4\n"

        val commandResult = GitCommandResult(false, 0, listOf(), listOf(branches))
        Mockito.doAnswer {
            project.guessProjectDir()
        }.`when`(utils).getRoot()
        Mockito.doAnswer {
            MockGitRepository("bee")
        }.`when`(utils).getRepository()

        Mockito.doAnswer {
            commandResult
        }.`when`(utils).runCommand(anyCustom())

        val list = controlledService.getBranchesExceptCheckedOut()
        assertEquals(list, listOf(b1, b2, b3, b4))
    }

    private inline fun <reified T> anyCustom(): T = Mockito.any(T::class.java)
}
