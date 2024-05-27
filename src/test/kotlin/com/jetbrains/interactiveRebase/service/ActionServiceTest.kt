package com.jetbrains.interactiveRebase.service

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.FixupCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.ReorderCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.services.RebaseInvoker
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
        modelService.branchInfo.initialCommits = mutableListOf(commitInfo1, commitInfo2)
        modelService.branchInfo.currentCommits = mutableListOf(commitInfo1, commitInfo2)
        modelService.addOrRemoveCommitSelection(commitInfo1)
        modelService.branchInfo.setName("feature1")
        modelService.branchInfo.addSelectedCommits(commitInfo1)
        modelService.invoker.branchInfo = modelService.branchInfo

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

    fun testPerformPickAction() {
        // setup the commands
        val command1 = SquashCommand(commitInfo1, mutableListOf(commitInfo2), "lol")
        val command2 = ReorderCommand(1, 2)
        commitInfo1.addChange(command1)
        commitInfo1.addChange(command2)
        commitInfo1.isSelected = true

        val command3 = DropCommand(commitInfo2)
        commitInfo2.addChange(command3)

        project.service<RebaseInvoker>().addCommand(command1)
        project.service<RebaseInvoker>().addCommand(command2)
        project.service<RebaseInvoker>().addCommand(command3)

        actionService.performPickAction()

        assertThat(commitInfo1.changes.size).isEqualTo(1)
        assertThat(modelService.branchInfo.selectedCommits.size).isEqualTo(0)
    }

    fun testPerformPickActionWithEmptyList() {
        actionService.performPickAction()
        assertThat(modelService.branchInfo.selectedCommits.size).isEqualTo(0)
    }

    fun testResetAllChangesAction() {
        // setup the commands
        val command1 = DropCommand(commitInfo1)
        val command2 = ReorderCommand(1, 2)
        commitInfo1.addChange(command1)
        commitInfo1.addChange(command2)
        commitInfo1.isSelected = true

        val command3 = DropCommand(commitInfo2)
        commitInfo2.addChange(command3)

        modelService.invoker.addCommand(command1)
        modelService.invoker.addCommand(command2)
        modelService.invoker.addCommand(command3)

        actionService.resetAllChangesAction()
        assertThat(commitInfo1.changes.size).isEqualTo(0)
        assertThat(commitInfo2.changes.size).isEqualTo(0)
        assertThat(project.service<RebaseInvoker>().commands.size).isEqualTo(0)
        assertThat(modelService.branchInfo.selectedCommits.size).isEqualTo(0)
    }

    fun testTakeFixupActionMultipleCommits() {
        commitInfo2.isSelected = true

        modelService.addOrRemoveCommitSelection(commitInfo2)

        actionService.takeFixupAction()
        assertThat(commitInfo1.changes.size).isEqualTo(1)
        assertThat(commitInfo2.changes.size).isEqualTo(1)
        assertThat(modelService.invoker.commands[0])
        val command = commitInfo1.changes[0] as FixupCommand
        assertThat(command.parentCommit == commitInfo1)
        assertThat(command.fixupCommits == listOf(commitInfo2))
    }

    fun testTakeFixupActionSingleCommit() {
        actionService.takeFixupAction()
        assertThat(commitInfo1.changes.size).isEqualTo(1)
        assertThat(commitInfo2.changes.size).isEqualTo(1)
        assertThat(modelService.invoker.commands[0])
        val command = commitInfo1.changes[0] as FixupCommand
        assertThat(command.parentCommit == commitInfo1)
        assertThat(command.fixupCommits == listOf(commitInfo2))
    }

    fun testClearFixupOnPick() {
        val command1 = FixupCommand(commitInfo1, mutableListOf(commitInfo2))
        commitInfo1.addChange(command1)
        commitInfo2.addChange(command1)
        commitInfo1.isSquashed = true
        commitInfo2.isSquashed = true

        modelService.branchInfo.currentCommits = mutableListOf(commitInfo1)

        modelService.invoker.addCommand(command1)

        actionService.clearFixupOnPick(command1, commitInfo1)
        assertThat(modelService.branchInfo.currentCommits).isEqualTo(listOf(commitInfo2, commitInfo1))
    }

    fun testClearSquashOnPick() {
        val command1 = SquashCommand(commitInfo1, mutableListOf(commitInfo2), "lol")
        commitInfo1.addChange(command1)
        commitInfo2.addChange(command1)
        commitInfo1.isSquashed = true
        commitInfo2.isSquashed = true

        modelService.branchInfo.currentCommits = mutableListOf(commitInfo1)

        modelService.invoker.addCommand(command1)

        actionService.clearSquashOnPick(command1, commitInfo1)
        assertThat(modelService.branchInfo.currentCommits).isEqualTo(listOf(commitInfo2, commitInfo1))
    }

    private inline fun <reified T> anyCustom(): T = ArgumentMatchers.any(T::class.java)
}
