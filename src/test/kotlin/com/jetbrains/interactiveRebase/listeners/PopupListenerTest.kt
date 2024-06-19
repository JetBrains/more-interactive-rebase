package com.jetbrains.interactiveRebase.listeners

import com.intellij.mock.MockVirtualFile
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.utils.gitUtils.IRGitUtils
import git4idea.merge.GitConflictResolver
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.mockito.Mockito
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.awt.event.ActionEvent
import java.awt.event.WindowEvent
import javax.swing.JDialog
import javax.swing.JLabel

class PopupListenerTest : BasePlatformTestCase() {
    private lateinit var popupListener: PopupListener
    private lateinit var modelService: ModelService
    private lateinit var gitUtils: IRGitUtils
    private lateinit var gitConflictResolver: GitConflictResolver

    override fun setUp() {
        super.setUp()
        modelService = project.service<ModelService>()
        gitUtils = mock(IRGitUtils::class.java)
        gitConflictResolver = mock(GitConflictResolver::class.java)
        doNothing().`when`(gitConflictResolver).mergeNoProceed()
        popupListener = PopupListener(project, gitUtils)
    }

    fun testEventIsNotWindowOpened() {
        val event = ActionEvent("bla", WindowEvent.WINDOW_CLOSING, "bla")
        popupListener.eventDispatched(event)

        assertThat(modelService.previousConflictCommit).isEqualTo("")
    }

    fun testEventIsWindowOpenedButNotConflict() {
        val event = ActionEvent("bla", WindowEvent.WINDOW_OPENED, "bla")
        val dialog = mock(JDialog::class.java)
        `when`(dialog.title).thenReturn("Not conflicts")
        event.source = dialog
        popupListener.eventDispatched(event)

        assertThat(modelService.previousConflictCommit).isEqualTo("")
    }

    fun testEventIsWindowOpenedAndConflictRepoNotNull() {
        val event = ActionEvent("bla", WindowEvent.WINDOW_OPENED, "bla")
        val dialog = mock(JDialog::class.java)
        `when`(dialog.title).thenReturn("Conflicts")
        event.source = dialog

        val commit = TestGitCommitProvider(project).createCommit("abcdefgh")
        val commitInfo = CommitInfo(commit, project, mutableListOf())
        modelService.branchInfo.initialCommits = mutableListOf(commitInfo)

        val root = MockVirtualFile("mockFile")
        `when`(gitUtils.getRoot()).thenReturn(root)
        `when`(gitUtils.getCurrentRebaseCommit(anyCustom(), anyCustom())).thenReturn("MockHash(string='abcdefgh')")

        popupListener.eventDispatched(event)

        assertThat(modelService.previousConflictCommit).isEqualTo("MockHash(string='abcdefgh')")
    }

    fun testEventIsWindowOpenedAndConflictRepoNull() {
        val event = ActionEvent("bla", WindowEvent.WINDOW_OPENED, "bla")
        val dialog = mock(JDialog::class.java)
        `when`(dialog.title).thenReturn("Conflicts")
        event.source = dialog

        val commit = TestGitCommitProvider(project).createCommit("abcdefgh")
        val commitInfo = CommitInfo(commit, project, mutableListOf())
        modelService.branchInfo.initialCommits = mutableListOf(commitInfo)

        val root = MockVirtualFile("mockFile")
        `when`(gitUtils.getCurrentRebaseCommit(anyCustom(), anyCustom())).thenReturn("MockHash(string='abcdefgh')")

        popupListener.eventDispatched(event)

        assertThat(modelService.previousConflictCommit).isEqualTo("")
    }

    fun testEventIsWindowOpenedAndSourceNotJDialog() {
        val event = ActionEvent("bla", WindowEvent.WINDOW_OPENED, "bla")
        val dialog = JLabel()
        event.source = dialog

        popupListener.eventDispatched(event)

        assertThat(modelService.previousConflictCommit).isEqualTo("")
        popupListener.dispose()
    }

    private inline fun <reified T> anyCustom(): T = Mockito.any(T::class.java)
}
