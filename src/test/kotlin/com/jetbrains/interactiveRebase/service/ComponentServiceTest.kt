package com.jetbrains.interactiveRebase.service

<<<<<<< HEAD
import com.intellij.mock.MockVirtualFile
=======
>>>>>>> efcefae (Rebase connection established)
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.OnePixelSplitter
import com.intellij.vcs.log.Hash
import com.intellij.vcs.log.VcsUser
import com.intellij.vcs.log.VcsUserRegistry
import com.intellij.vcs.log.impl.VcsUserImpl
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.services.ComponentService
<<<<<<< HEAD
import com.jetbrains.interactiveRebase.threads.BranchInfoThread
=======
import com.jetbrains.interactiveRebase.threads.CommitInfoThread
import com.jetbrains.interactiveRebase.visuals.HeaderPanel
>>>>>>> efcefae (Rebase connection established)
import com.jetbrains.interactiveRebase.visuals.LabeledBranchPanel
import git4idea.GitCommit
import git4idea.history.GitCommitRequirements
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.awt.BorderLayout
import java.awt.GridBagLayout

class ComponentServiceTest : BasePlatformTestCase() {
    lateinit var componentService: ComponentService

    override fun setUp() {
        super.setUp()
        componentService = ComponentService(project)
    }

    fun testUpdateMainPanelVisuals() {
        componentService.updateMainPanelVisuals()

        assertEquals(1, componentService.mainComponent.componentCount)
        assertTrue(componentService.mainComponent.getComponent(0) is OnePixelSplitter)
    }

    fun testCreateMainComponent() {
        val mainComponent = componentService.createMainComponent()

        assertTrue(mainComponent.layout is BorderLayout)
    }

    fun testCreateBranchPanel() {
        val branchPanel = componentService.createBranchPanel()

        assertTrue(branchPanel.layout is GridBagLayout)
        assertEquals(1, branchPanel.componentCount)
        assertTrue(branchPanel.getComponent(0) is LabeledBranchPanel)
    }

    fun testAddOrRemoveCommitSelection() {
        val commit1 = CommitInfo(createCommit("my commit"), project, null)
        commit1.isSelected = true

        val res = componentService.addOrRemoveCommitSelection(commit1)
        assertEquals(componentService.branchInfo.selectedCommits, listOf(commit1))
    }

    fun testAddOrRemoveCommitSelectionCommitIsNotSelected() {
        val commit1 = mock(CommitInfo::class.java)
        `when`(commit1.isSelected).thenReturn(false)
        componentService.branchInfo.selectedCommits = mutableListOf(commit1)

        val res = componentService.addOrRemoveCommitSelection(commit1)
        assertEquals(componentService.branchInfo.selectedCommits.size, 0)
    }

    fun testGetSelectedCommits() {
        val commit1 = mock(CommitInfo::class.java)
        val commit2 = mock(CommitInfo::class.java)
        componentService.branchInfo.selectedCommits = mutableListOf(commit1, commit2)

        val res = componentService.getSelectedCommits()
        assertEquals(res, listOf(commit1, commit2))
    }

    fun testUpdateMainComponentThread() {
        val mockThread = mock(BranchInfoThread::class.java)
        doNothing().`when`(mockThread).join()
        doNothing().`when`(mockThread).start()

        val updated = componentService.refresh()

        assertEquals(1, updated.componentCount)
        assertEquals(OnePixelSplitter::class.java, updated.getComponent(0).javaClass)

        // TODO: Find a way to test the actual thread
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
}
