package com.jetbrains.interactiveRebase.service

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.EDT
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.visuals.CommitInfoPanel
import com.jetbrains.interactiveRebase.visuals.MainPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.mock

class ActionServiceTest : BasePlatformTestCase() {
    private lateinit var mainPanel: MainPanel
    private lateinit var modelService: ModelService
    private lateinit var commitInfo1: CommitInfo
    private lateinit var commitInfo2: CommitInfo
    private lateinit var branchInfo: BranchInfo
    private lateinit var actionService: ActionService

    override fun setUp() {
        super.setUp()
        val commitProvider = TestGitCommitProvider(project)
        commitInfo1 = CommitInfo(commitProvider.createCommit("tests"), project, mutableListOf())
        commitInfo2 = CommitInfo(commitProvider.createCommit("fix tests"), project, mutableListOf())
        val commitService = mock(CommitService::class.java)

        Mockito.doAnswer {
            listOf(commitInfo1.commit)
        }.`when`(commitService).getCommits()

        Mockito.doAnswer {
            "feature1"
        }.`when`(commitService).getBranchName()

        modelService = ModelService(project, CoroutineScope(Dispatchers.EDT), commitService)
        modelService.addOrRemoveCommitSelection(commitInfo1)
        modelService.branchInfo.setName("feature1")
        modelService.branchInfo.addSelectedCommits(commitInfo1)
        branchInfo = modelService.branchInfo
        mainPanel = MainPanel(project, branchInfo, modelService.invoker)
        mainPanel.commitInfoPanel = mock(CommitInfoPanel::class.java)
        Mockito.doNothing().`when`(mainPanel.commitInfoPanel).commitsSelected(anyCustom())
        Mockito.doNothing().`when`(mainPanel.commitInfoPanel).repaint()

        actionService = ActionService(project, modelService, modelService.invoker)
    }

    fun testTakeDropAction() {
        actionService.takeDropAction()
        assertThat(branchInfo.selectedCommits.isEmpty()).isTrue()
        assertThat(commitInfo1.changes.isNotEmpty()).isTrue()
        assertThat(commitInfo1.isSelected).isFalse()
        Mockito.verify(mainPanel.commitInfoPanel).commitsSelected(anyCustom())
    }

    fun testTakeRewordAction() {
        actionService.takeRewordAction()
        assertThat(commitInfo1.isDoubleClicked).isTrue()
        assertThat(commitInfo2.isDoubleClicked).isFalse()
    }

    fun testTakeRewordActionConsidersEmptyList() {
        commitInfo1.isSelected = false
        modelService.branchInfo.clearSelectedCommits()
        actionService.takeRewordAction()
        assertThat(commitInfo2.isDoubleClicked).isFalse()
        assertThat(commitInfo1.isDoubleClicked).isFalse()
    }

    fun testCheckRewordDisables() {
        commitInfo1.isSelected = false
        modelService.branchInfo.clearSelectedCommits()
        val presentation = Presentation()
        presentation.isEnabled = true
        val event = createEventWithPresentation(presentation)
        actionService.checkReword(event)
        assertThat(presentation.isEnabledAndVisible).isFalse()
        modelService.branchInfo.addSelectedCommits(commitInfo1)
        actionService.checkReword(event)
        assertThat(presentation.isEnabledAndVisible).isTrue()
        modelService.branchInfo.addSelectedCommits(commitInfo2)
        actionService.checkReword(event)
        assertThat(presentation.isEnabledAndVisible).isFalse()
    }

    fun testCheckDropDisables() {
        commitInfo1.isSelected = false
        modelService.branchInfo.clearSelectedCommits()
        val presentation = Presentation()
        presentation.isEnabledAndVisible = true
        val event = createEventWithPresentation(presentation)
        actionService.checkDrop(event)
        assertThat(presentation.isEnabledAndVisible).isFalse()
        modelService.branchInfo.addSelectedCommits(commitInfo1)
        actionService.checkDrop(event)
        assertThat(presentation.isEnabledAndVisible).isTrue()
    }

    private fun createEventWithPresentation(presentation: Presentation): AnActionEvent {
        val dataContext = mock(DataContext::class.java)
        return AnActionEvent(null, dataContext, "place", presentation, ActionManager.getInstance(), 2)
    }

    fun testTakeStopToEditAction() {
        actionService.takeStopToEditAction()
        assertThat(branchInfo.selectedCommits.isEmpty()).isTrue()
        assertThat(commitInfo1.changes.isNotEmpty()).isTrue()
        assertThat(commitInfo1.isSelected).isFalse()
        Mockito.verify(mainPanel.commitInfoPanel).commitsSelected(anyCustom())
    }

    private inline fun <reified T> anyCustom(): T = ArgumentMatchers.any(T::class.java)
}
