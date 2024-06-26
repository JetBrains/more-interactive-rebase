package com.jetbrains.interactiveRebase.service

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.testFramework.TestActionEvent.createTestEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.actions.gitPanel.RewordAction
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.GraphInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.CherryCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.CollapseCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.FixupCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.PickCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.RebaseCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.ReorderCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.RewordCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import com.jetbrains.interactiveRebase.visuals.CommitInfoPanel
import com.jetbrains.interactiveRebase.visuals.GraphPanel
import com.jetbrains.interactiveRebase.visuals.MainPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.mock

class ActionServiceTest : BasePlatformTestCase() {
    private lateinit var mainPanel: MainPanel
    private lateinit var modelService: ModelService
    private lateinit var commitInfo1: CommitInfo
    private lateinit var commitInfo2: CommitInfo
    private lateinit var commitInfo6: CommitInfo
    private lateinit var branchInfo: BranchInfo
    private lateinit var commitInfo3: CommitInfo
    private lateinit var commitInfo4: CommitInfo
    private lateinit var commitInfo5: CommitInfo
    private lateinit var actionService: ActionService
    private var addedBranch: BranchInfo = BranchInfo()

    override fun setUp() {
        super.setUp()
        val commitProvider = TestGitCommitProvider(project)
        commitInfo1 = CommitInfo(commitProvider.createCommit("tests"), project, mutableListOf())
        commitInfo2 = CommitInfo(commitProvider.createCommit("fix tests"), project, mutableListOf())
        commitInfo6 = CommitInfo(commitProvider.createCommit("fix tests yeah"), project, mutableListOf())
        commitInfo3 = CommitInfo(commitProvider.createCommit("belongs to added branch"), project, mutableListOf())
        commitInfo4 = CommitInfo(commitProvider.createCommit("belongs to added branch too"), project, mutableListOf())
        commitInfo5 = CommitInfo(commitProvider.createCommit("belongs to added branch on the top"), project, mutableListOf())

        val commitService = mock(CommitService::class.java)

        Mockito.doAnswer {
            listOf(commitInfo1.commit)
        }.`when`(commitService).getCommits(anyString())

        Mockito.doAnswer {
            "feature1"
        }.`when`(commitService).getBranchName()

        modelService = ModelService(project, CoroutineScope(Dispatchers.EDT), commitService)
        modelService.branchInfo.initialCommits = mutableListOf(commitInfo1, commitInfo2, commitInfo6)
        modelService.branchInfo.currentCommits = mutableListOf(commitInfo1, commitInfo2, commitInfo6)
        addedBranch.name = "added"
        addedBranch.currentCommits = mutableListOf(commitInfo5, commitInfo4, commitInfo3)
        addedBranch.initialCommits = mutableListOf(commitInfo5, commitInfo4, commitInfo3)
        addedBranch.baseCommit = commitInfo3
        modelService.graphInfo = GraphInfo(modelService.branchInfo, addedBranch)
        modelService.addToSelectedCommits(commitInfo1, modelService.branchInfo)

        modelService.branchInfo.setName("feature1")
        modelService.invoker.branchInfo = modelService.branchInfo

        branchInfo = modelService.branchInfo
        mainPanel = MainPanel(project)
        mainPanel.commitInfoPanel = mock(CommitInfoPanel::class.java)
        mainPanel.graphPanel = GraphPanel(project, modelService.graphInfo)
        Mockito.doNothing().`when`(mainPanel.commitInfoPanel).commitsSelected(anyCustom())
        Mockito.doNothing().`when`(mainPanel.commitInfoPanel).repaint()
        actionService = ActionService(project, modelService, modelService.invoker)
        actionService.mainPanel = mainPanel
    }

    fun testTakeDropAction() {
        actionService.takeDropAction()
        assertThat(branchInfo.selectedCommits.isEmpty()).isTrue()
        assertThat(commitInfo1.changes.isNotEmpty()).isTrue()
        assertThat(commitInfo1.isSelected).isFalse()
        modelService.branchInfo.selectedCommits.add(commitInfo2)
        actionService.takeDropAction()
        assertThat(commitInfo6.changes.size).isEqualTo(0)

        commitInfo6.changes.clear()
        modelService.branchInfo.selectedCommits.add(commitInfo6)
        modelService.branchInfo.selectedCommits.add(commitInfo3)
        commitInfo6.isSquashed = false
        commitInfo3.isSquashed = true
        actionService.takeDropAction()
        assertThat(commitInfo6.changes.size).isEqualTo(1)
        assertThat(commitInfo3.changes.size).isEqualTo(0)
    }

    fun testTakeRewordAction() {
        actionService.takeRewordAction()
        assertThat(commitInfo1.isTextFieldEnabled).isTrue()
        assertThat(commitInfo2.isTextFieldEnabled).isFalse()

        modelService.branchInfo.selectedCommits.add(commitInfo2)
        modelService.branchInfo.selectedCommits.add(commitInfo1)
        commitInfo1.isSquashed = true
        commitInfo2.isSquashed = false
        actionService.takeRewordAction()
        assertThat(commitInfo1.isTextFieldEnabled).isTrue()
        assertThat(commitInfo2.isTextFieldEnabled).isTrue()
    }

    fun testTakeRewordActionConsidersEmptyList() {
        commitInfo1.isSelected = false
        modelService.branchInfo.clearSelectedCommits()
        actionService.takeRewordAction()
        assertThat(commitInfo2.isTextFieldEnabled).isFalse()
        assertThat(commitInfo1.isTextFieldEnabled).isFalse()
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
        modelService.rebaseInProcess = true
        actionService.checkReword(event)
        assertThat(presentation.isEnabledAndVisible).isFalse()
        modelService.rebaseInProcess = false
        modelService.branchInfo.addSelectedCommits(commitInfo2)
        actionService.checkReword(event)
        assertThat(presentation.isEnabledAndVisible).isFalse()
        commitInfo2.addChange(DropCommand(commitInfo2))
        modelService.branchInfo.removeSelectedCommits(commitInfo1)
        actionService.checkReword(event)
        assertThat(presentation.isEnabledAndVisible).isFalse()

        modelService.rebaseInProcess = true
        actionService.checkReword(event)
        assertThat(event.presentation.isEnabledAndVisible).isFalse()
        modelService.rebaseInProcess = false

        branchInfo.selectedCommits.add(commitInfo1)
        addedBranch.selectedCommits.add(commitInfo5)
        actionService.checkReword(event)
        assertThat(event.presentation.isEnabledAndVisible).isFalse()

        branchInfo.selectedCommits.add(commitInfo1)
        modelService.rebaseInProcess = true
        actionService.checkReword(event)
        assertThat(event.presentation.isEnabledAndVisible).isFalse()
    }

    fun testCheckStopToEditDisables() {
        commitInfo1.isSelected = false
        modelService.branchInfo.clearSelectedCommits()
        val presentation = Presentation()
        presentation.isEnabled = true
        val event = createEventWithPresentation(presentation)
        actionService.checkStopToEdit(event)
        assertThat(presentation.isEnabledAndVisible).isFalse()
        modelService.branchInfo.addSelectedCommits(commitInfo1)
        actionService.checkReword(event)
        assertThat(presentation.isEnabledAndVisible).isTrue()
        modelService.rebaseInProcess = true
        actionService.checkReword(event)
        assertThat(presentation.isEnabledAndVisible).isFalse()
        modelService.rebaseInProcess = false
        commitInfo1.addChange(DropCommand(commitInfo1))
        actionService.checkStopToEdit(event)
        assertThat(presentation.isEnabledAndVisible).isFalse()

        branchInfo.selectedCommits.add(commitInfo1)
        addedBranch.selectedCommits.add(commitInfo5)
        actionService.checkStopToEdit(event)
        assertThat(presentation.isEnabledAndVisible).isFalse()

        branchInfo.selectedCommits.add(commitInfo1)
        modelService.rebaseInProcess = true
        actionService.checkStopToEdit(event)
        assertThat(presentation.isEnabledAndVisible).isFalse()

        modelService.rebaseInProcess = true
        actionService.checkStopToEdit(event)
        assertThat(event.presentation.isEnabledAndVisible).isFalse()
        modelService.rebaseInProcess = false
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
        modelService.rebaseInProcess = true
        actionService.checkDrop(event)
        assertThat(presentation.isEnabledAndVisible).isFalse()
        modelService.rebaseInProcess = false
        commitInfo1.addChange(DropCommand(commitInfo1))
        actionService.checkDrop(event)
        assertThat(presentation.isEnabledAndVisible).isFalse()

        modelService.rebaseInProcess = true
        actionService.checkDrop(event)
        assertThat(event.presentation.isEnabledAndVisible).isFalse()

        branchInfo.selectedCommits.add(commitInfo1)
        addedBranch.selectedCommits.add(commitInfo5)
        actionService.checkDrop(event)
        assertThat(event.presentation.isEnabledAndVisible).isFalse()

        addedBranch.selectedCommits.add(commitInfo5)
        actionService.checkDrop(event)
        assertThat(event.presentation.isEnabledAndVisible).isFalse()

        branchInfo.selectedCommits.add(commitInfo1)
        modelService.rebaseInProcess = true
        actionService.checkDrop(event)
        assertThat(event.presentation.isEnabledAndVisible).isFalse()
    }

    fun testCheckFixupOrSquashDisables() {
        commitInfo1.isSelected = false
        modelService.branchInfo.clearSelectedCommits()
        addedBranch.clearSelectedCommits()
        val e = createTestEvent()
        actionService.checkFixupOrSquash(e)
        assertThat(e.presentation.isEnabledAndVisible).isFalse()
        modelService.branchInfo.addSelectedCommits(commitInfo2)
        actionService.checkFixupOrSquash(e)
        assertThat(e.presentation.isEnabledAndVisible).isTrue()
        modelService.branchInfo.addSelectedCommits(commitInfo1)
        actionService.checkStopToEdit(e)
        assertThat(e.presentation.isEnabledAndVisible).isTrue()

        modelService.rebaseInProcess = true
        actionService.checkFixupOrSquash(e)
        assertThat(e.presentation.isEnabledAndVisible).isFalse()
        modelService.rebaseInProcess = false
        commitInfo1.addChange(DropCommand(commitInfo1))
        actionService.checkStopToEdit(e)
        assertThat(e.presentation.isEnabledAndVisible).isFalse()

        modelService.rebaseInProcess = true
        actionService.checkStopToEdit(e)
        assertThat(e.presentation.isEnabledAndVisible).isFalse()

        modelService.branchInfo.addSelectedCommits(commitInfo6)
        actionService.checkFixupOrSquash(e)
        assertThat(e.presentation.isEnabledAndVisible).isFalse()

        modelService.branchInfo.addSelectedCommits(commitInfo2)
        commitInfo2.addChange(DropCommand(commitInfo2))
        commitInfo2.addChange(PickCommand(commitInfo2))
        commitInfo2.addChange(DropCommand(commitInfo2))
        actionService.checkFixupOrSquash(e)
        assertThat(e.presentation.isEnabledAndVisible).isFalse()

        modelService.branchInfo.addSelectedCommits(commitInfo2)
        commitInfo2.addChange(DropCommand(commitInfo2))
        commitInfo2.addChange(PickCommand(commitInfo2))
        addedBranch.selectedCommits.add(commitInfo5)
        actionService.checkFixupOrSquash(e)
        assertThat(e.presentation.isEnabledAndVisible).isFalse()

        modelService.branchInfo.addSelectedCommits(commitInfo6)
        commitInfo6.addChange(DropCommand(commitInfo6))
        commitInfo6.addChange(PickCommand(commitInfo6))
        actionService.checkFixupOrSquash(e)
        assertThat(e.presentation.isEnabledAndVisible).isFalse()

        modelService.branchInfo.addSelectedCommits(commitInfo2)
        commitInfo2.addChange(DropCommand(commitInfo2))
        commitInfo2.addChange(PickCommand(commitInfo2))
        commitInfo2.isCollapsed = true
        actionService.checkFixupOrSquash(e)
        assertThat(e.presentation.isEnabledAndVisible).isFalse()

        modelService.branchInfo.addSelectedCommits(commitInfo2)
        commitInfo2.addChange(DropCommand(commitInfo2))
        commitInfo2.addChange(PickCommand(commitInfo2))
        commitInfo2.addChange(DropCommand(commitInfo2))
        actionService.checkFixupOrSquash(e)
        assertThat(e.presentation.isEnabledAndVisible).isFalse()
    }

    fun testCheckPickDisables() {
        commitInfo1.isSelected = false
        modelService.branchInfo.clearSelectedCommits()
        val presentation = Presentation()
        presentation.isEnabled = true
        val event = createEventWithPresentation(presentation)
        actionService.checkPick(event)
        assertThat(presentation.isEnabledAndVisible).isFalse()
        modelService.branchInfo.addSelectedCommits(commitInfo2)
        actionService.checkPick(event)
        assertThat(presentation.isEnabledAndVisible).isTrue()
        modelService.rebaseInProcess = true
        actionService.checkPick(event)
        assertThat(presentation.isEnabledAndVisible).isFalse()
        modelService.rebaseInProcess = false

        modelService.rebaseInProcess = true
        actionService.checkPick(event)
        assertThat(event.presentation.isEnabledAndVisible).isFalse()

        branchInfo.selectedCommits.add(commitInfo2)
        addedBranch.selectedCommits.add(commitInfo5)
        actionService.checkPick(event)
        assertThat(event.presentation.isEnabledAndVisible).isFalse()

        branchInfo.selectedCommits.add(commitInfo2)
        modelService.rebaseInProcess = true
        actionService.checkPick(event)
        assertThat(event.presentation.isEnabledAndVisible).isFalse()
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
        branchInfo.selectedCommits.add(commitInfo2)
        commitInfo2.isSquashed = true
        assertThat(commitInfo2.changes.isEmpty()).isTrue()
    }

    fun testPerformPickAction() {
        // setup the commands
        val command1 = SquashCommand(commitInfo1, mutableListOf(commitInfo2), "lol")
        val command2 = ReorderCommand(commitInfo1, 1, 2)
        commitInfo1.addChange(command1)
        commitInfo1.addChange(command2)
        commitInfo1.isSelected = true

        val command3 = DropCommand(commitInfo2)
        commitInfo2.addChange(command3)

        project.service<RebaseInvoker>().addCommand(command1)
        project.service<RebaseInvoker>().addCommand(command2)
        project.service<RebaseInvoker>().addCommand(command3)

        actionService.performPickAction()

        assertThat(commitInfo1.changes.size).isEqualTo(3)
        assertThat(commitInfo1.changes[2]).isInstanceOf(PickCommand::class.java)
        assertThat(modelService.branchInfo.selectedCommits.size).isEqualTo(0)

        val c0 = PickCommand(commitInfo6)
        val c1 = ReorderCommand(commitInfo6, 1, 2)
        commitInfo6.addChange(c0)
        commitInfo6.addChange(c1)
        commitInfo6.isSelected = true
        branchInfo.selectedCommits.add(commitInfo6)

        project.service<RebaseInvoker>().commands.clear()
        project.service<RebaseInvoker>().addCommand(c0)
        project.service<RebaseInvoker>().addCommand(c1)

        actionService.performPickAction()

        assertThat(project.service<RebaseInvoker>().commands.size).isEqualTo(3)

        val c2 = FixupCommand(commitInfo6, mutableListOf(commitInfo2))
        val c3 = ReorderCommand(commitInfo6, 1, 2)
        commitInfo6.addChange(c2)
        commitInfo6.addChange(c3)
        commitInfo2.addChange(c2)
        commitInfo6.isSelected = true
        branchInfo.selectedCommits.add(commitInfo2)

        project.service<RebaseInvoker>().commands.clear()
        project.service<RebaseInvoker>().addCommand(c2)
        project.service<RebaseInvoker>().addCommand(c3)

        actionService.performPickAction()

        assertThat(project.service<RebaseInvoker>().commands.size).isEqualTo(3)

        val c4 = SquashCommand(commitInfo6, mutableListOf(commitInfo2), "lol")
        val c5 = ReorderCommand(commitInfo6, 1, 2)
        commitInfo6.addChange(c4)
        commitInfo6.addChange(c5)
        commitInfo2.addChange(c4)
        commitInfo6.isSelected = true
        branchInfo.selectedCommits.add(commitInfo2)

        project.service<RebaseInvoker>().commands.clear()
        project.service<RebaseInvoker>().addCommand(c4)
        project.service<RebaseInvoker>().addCommand(c5)

        actionService.performPickAction()

        assertThat(project.service<RebaseInvoker>().commands.size).isEqualTo(3)

        val c6 = SquashCommand(commitInfo3, mutableListOf(commitInfo2), "lol")
        val c7 = ReorderCommand(commitInfo6, 1, 2)
        commitInfo3.addChange(c6)
        commitInfo6.addChange(c7)
        commitInfo2.addChange(c6)
        commitInfo6.isSelected = true
        branchInfo.selectedCommits.add(commitInfo3)

        project.service<RebaseInvoker>().commands.clear()
        project.service<RebaseInvoker>().addCommand(c6)
        project.service<RebaseInvoker>().addCommand(c7)

        actionService.performPickAction()

        assertThat(branchInfo.currentCommits.size).isEqualTo(4)

        val c8 = FixupCommand(commitInfo3, mutableListOf(commitInfo2))
        val c9 = ReorderCommand(commitInfo6, 1, 2)
        commitInfo3.addChange(c8)
        commitInfo6.addChange(c9)
        commitInfo2.addChange(c8)
        commitInfo6.isSelected = true
        branchInfo.selectedCommits.add(commitInfo3)

        project.service<RebaseInvoker>().commands.clear()
        project.service<RebaseInvoker>().addCommand(c8)
        project.service<RebaseInvoker>().addCommand(c9)

        actionService.performPickAction()

        assertThat(branchInfo.currentCommits.size).isEqualTo(4)
    }

    fun testPerformPickActionForFixUp() {
        modelService.invoker.undoneCommands.clear()
        modelService.invoker.commands.clear()

        val command1 = FixupCommand(commitInfo1, mutableListOf(commitInfo2))

        commitInfo1.addChange(command1)
        commitInfo2.addChange(command1)

        commitInfo1.isSelected = true

        modelService.branchInfo.currentCommits = mutableListOf(commitInfo1)
        modelService.invoker.addCommand(command1)

        actionService.performPickAction()
        assertThat(modelService.invoker.commands.size).isEqualTo(2)

        assertThat(modelService.branchInfo.currentCommits).isEqualTo(mutableListOf(commitInfo2, commitInfo1))
    }

    fun testPerformPickActionWithEmptyList() {
        actionService.performPickAction()
        assertThat(modelService.branchInfo.selectedCommits.size).isEqualTo(0)
    }

    fun testResetAllChangesAction() {
        // setup the commands
        val command1 = DropCommand(commitInfo1)
        val command2 = ReorderCommand(commitInfo1, 1, 2)
        modelService.addToSelectedCommits(commitInfo3, addedBranch)
        commitInfo3.isSelected = true
        commitInfo3.wasCherryPicked = true
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
        assertThat(addedBranch.selectedCommits.size).isEqualTo(0)
        assertFalse(commitInfo3.isSelected)
        assertFalse(commitInfo3.isSelected)
    }

    fun testTakeFixupActionNested() {
        modelService.invoker.commands.clear()
        modelService.clearSelectedCommits()
        modelService.addToSelectedCommits(commitInfo2, branchInfo)
        actionService.takeFixupAction()
        modelService.addToSelectedCommits(commitInfo1, branchInfo)
        actionService.takeFixupAction()
        assertThat(modelService.invoker.commands.size).isEqualTo(1)
        assertThat(modelService.invoker.commands[0]).isInstanceOf(FixupCommand::class.java)
        val command = commitInfo1.changes[0] as FixupCommand
        assertThat(command.parentCommit).isEqualTo(commitInfo6)
        assertThat(command.fixupCommits).isEqualTo(listOf(commitInfo1, commitInfo2))
    }

    fun testTakeSquashActionNested() {
        modelService.invoker.commands.clear()
        modelService.clearSelectedCommits()
        modelService.addToSelectedCommits(commitInfo2, branchInfo)
        actionService.takeSquashAction()
        modelService.addToSelectedCommits(commitInfo1, branchInfo)
        actionService.takeSquashAction()
        assertThat(modelService.invoker.commands.size).isEqualTo(1)
        assertThat(modelService.invoker.commands[0]).isInstanceOf(SquashCommand::class.java)
        val command = commitInfo1.changes[0] as SquashCommand
        assertThat(command.parentCommit).isEqualTo(commitInfo6)
        assertThat(command.squashedCommits).isEqualTo(listOf(commitInfo1, commitInfo2))
    }

    fun testTakeFixupActionMultipleCommits() {
        modelService.invoker.commands.clear()
        modelService.addToSelectedCommits(commitInfo2, branchInfo)
        actionService.takeFixupAction()
        assertThat(modelService.invoker.commands[0]).isInstanceOf(FixupCommand::class.java)
        val command = commitInfo1.changes[0] as FixupCommand
        assertThat(command.parentCommit).isEqualTo(commitInfo2)
        assertThat(command.fixupCommits).isEqualTo(listOf(commitInfo1))
    }

    fun testTakeSquashActionMultipleCommits() {
        modelService.invoker.commands.clear()

        modelService.addToSelectedCommits(commitInfo2, branchInfo)
        actionService.takeSquashAction()

        assertThat(modelService.invoker.commands[0]).isInstanceOf(SquashCommand::class.java)
        val command = commitInfo1.changes[0] as SquashCommand
        assertThat(command.parentCommit).isEqualTo(commitInfo2)
        assertThat(command.squashedCommits).isEqualTo(listOf(commitInfo1))
    }

    fun testTakeFixupActionSingleCommit() {
        modelService.invoker.commands.clear()
        actionService.takeFixupAction()
        assertThat(commitInfo1.changes.size).isEqualTo(1)
        assertThat(commitInfo2.changes.size).isEqualTo(1)
        assertThat(modelService.invoker.commands[0]).isInstanceOf(FixupCommand::class.java)
        val command = commitInfo1.changes[0] as FixupCommand
        assertThat(command.parentCommit).isEqualTo(commitInfo2)
        assertThat(command.fixupCommits).isEqualTo(listOf(commitInfo1))
    }

    fun testTakeSquashActionSingleCommit() {
        modelService.invoker.commands.clear()
        actionService.takeSquashAction()
        assertThat(commitInfo1.changes).hasSize(1)
        assertThat(commitInfo2.changes).hasSize(1)
        assertThat(modelService.invoker.commands[0]).isInstanceOf(SquashCommand::class.java)
        val command = modelService.invoker.commands[0] as SquashCommand
        assertThat(commitInfo1.changes[0]).isEqualTo(command)
        assertThat(commitInfo2.changes[0]).isEqualTo(command)
        assertThat(command.parentCommit).isEqualTo(commitInfo2)
        assertThat(command.squashedCommits).isEqualTo(listOf(commitInfo1))
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

    fun testCombinedCommits() {
        val squash = SquashCommand(commitInfo1, mutableListOf(commitInfo2), "new message")
        assertThat(actionService.getCombinedCommits(squash)).isEqualTo(mutableListOf(commitInfo2))
        val fixup = FixupCommand(commitInfo2, mutableListOf(commitInfo1))
        assertThat(actionService.getCombinedCommits(fixup)).isEqualTo(mutableListOf(commitInfo1))
        val neither = RewordCommand(commitInfo1, "new")
        assertThat(actionService.getCombinedCommits(neither)).isEmpty()
    }

    fun testCheckUndoDisabled() {
        modelService.invoker.commands.clear()
        val testEvent = createTestEvent()
        actionService.checkIfChangesMade(testEvent)
        assertThat(testEvent.presentation.isEnabled).isFalse()
    }

    fun testCheckRebaseAndResetDisabled() {
        modelService.invoker.commands.clear()
        val testEvent = createTestEvent()
        actionService.checkRebaseAndReset(testEvent)
        assertThat(testEvent.presentation.isEnabled).isFalse()
    }

    fun testCheckRebaseAndResetEnabled() {
        modelService.invoker.commands.clear()
        val command1 = RewordCommand(commitInfo1, "reorderTest")
        modelService.invoker.addCommand(command1)
        val testEvent = createTestEvent()
        actionService.checkRebaseAndReset(testEvent)
        assertThat(testEvent.presentation.isEnabled).isTrue()
    }

    fun testCheckUndoEnabled() {
        modelService.invoker.commands.clear()
        val command1 = RewordCommand(commitInfo1, "reorderTest")
        modelService.invoker.addCommand(command1)
        val testEvent = createTestEvent()
        actionService.checkIfChangesMade(testEvent)
        assertThat(testEvent.presentation.isEnabled).isTrue()
    }

    fun testCheckRedoDisabled() {
        modelService.invoker.undoneCommands.clear()
        val testEvent = createTestEvent()
        actionService.checkRedo(testEvent)
        assertThat(testEvent.presentation.isEnabled).isFalse()
    }

    fun testCheckRedoEnabled() {
        val command1 = RewordCommand(commitInfo1, "reorderTest")
        modelService.invoker.undoneCommands.add(command1)
        val testEvent = createTestEvent()
        actionService.checkRedo(testEvent)
        assertThat(testEvent.presentation.isEnabled).isTrue()
    }

    fun testUndoReorder() {
        modelService.invoker.undoneCommands.clear()
        modelService.invoker.commands.clear()
        val command1 = RewordCommand(commitInfo1, "reorderTest")
        val command2 = ReorderCommand(commitInfo1, 0, 1)
        commitInfo1.addChange(command1)
        commitInfo1.addChange(command2)

        modelService.branchInfo.currentCommits = mutableListOf(commitInfo2, commitInfo1)
        modelService.invoker.addCommand(command1)
        modelService.invoker.addCommand(command2)
        actionService.undoLastAction()
        assertThat(modelService.invoker.commands.size).isEqualTo(1)
        assertThat(modelService.invoker.undoneCommands.size).isEqualTo(1)

        assertThat(modelService.branchInfo.currentCommits).isEqualTo(mutableListOf(commitInfo1, commitInfo2))
    }

    fun testRedoReorder() {
        modelService.invoker.undoneCommands.clear()
        modelService.invoker.commands.clear()
        val command1 = RewordCommand(commitInfo1, "reorderTest")
        val command2 = ReorderCommand(commitInfo1, 1, 0)
        commitInfo1.addChange(command1)
        commitInfo1.addChange(command2)

        modelService.branchInfo.currentCommits = mutableListOf(commitInfo2, commitInfo1)
        modelService.invoker.addCommand(command1)
        modelService.invoker.undoneCommands.add(command2)
        actionService.redoLastAction()
        assertThat(modelService.invoker.commands.size).isEqualTo(2)
        assertThat(modelService.invoker.undoneCommands.size).isEqualTo(0)

        assertThat(modelService.branchInfo.currentCommits).isEqualTo(mutableListOf(commitInfo1, commitInfo2))
    }

    fun testUndoPickWithSquashBefore() {
        modelService.invoker.undoneCommands.clear()
        modelService.invoker.commands.clear()

        val command1 = SquashCommand(commitInfo1, mutableListOf(commitInfo2), "new message")
        val command2 = PickCommand(commitInfo1)
        val command3 = PickCommand(commitInfo2)

        commitInfo1.addChange(command1)
        commitInfo2.addChange(command1)
        commitInfo1.addChange(command2)
        commitInfo2.addChange(command3)

        modelService.branchInfo.currentCommits = mutableListOf(commitInfo2, commitInfo1)
        modelService.invoker.addCommand(command1)
        modelService.invoker.addCommand(command3)

        actionService.undoLastAction()
        assertThat(modelService.invoker.commands.size).isEqualTo(1)
        assertThat(modelService.invoker.undoneCommands.size).isEqualTo(1)

        assertThat(modelService.branchInfo.currentCommits).isEqualTo(mutableListOf(commitInfo1))
    }

    fun testUndoPickWithFixupBefore() {
        modelService.invoker.undoneCommands.clear()
        modelService.invoker.commands.clear()

        val command1 = FixupCommand(commitInfo1, mutableListOf(commitInfo2))
        val command2 = PickCommand(commitInfo1)
        val command3 = PickCommand(commitInfo2)

        commitInfo1.addChange(command1)
        commitInfo2.addChange(command1)
        commitInfo1.addChange(command2)
        commitInfo2.addChange(command3)

        modelService.branchInfo.currentCommits = mutableListOf(commitInfo2, commitInfo1)
        modelService.invoker.addCommand(command1)
        modelService.invoker.addCommand(command3)

        actionService.undoLastAction()
        assertThat(modelService.invoker.commands.size).isEqualTo(1)
        assertThat(modelService.invoker.undoneCommands.size).isEqualTo(1)

        assertThat(modelService.branchInfo.currentCommits).isEqualTo(mutableListOf(commitInfo1))
    }

    fun testUndoSquash() {
        modelService.invoker.undoneCommands.clear()
        modelService.invoker.commands.clear()

        val command1 = SquashCommand(commitInfo1, mutableListOf(commitInfo2), "new message")

        commitInfo1.addChange(command1)
        commitInfo2.addChange(command1)

        modelService.branchInfo.currentCommits = mutableListOf(commitInfo1)
        modelService.invoker.addCommand(command1)

        actionService.undoLastAction()
        assertThat(modelService.invoker.commands.size).isEqualTo(0)
        assertThat(modelService.invoker.undoneCommands.size).isEqualTo(1)

        assertThat(modelService.branchInfo.currentCommits).isEqualTo(mutableListOf(commitInfo2, commitInfo1))
    }

    fun testUndoFixup() {
        modelService.invoker.undoneCommands.clear()
        modelService.invoker.commands.clear()

        val command1 = FixupCommand(commitInfo1, mutableListOf(commitInfo2))

        commitInfo1.addChange(command1)
        commitInfo2.addChange(command1)

        modelService.branchInfo.currentCommits = mutableListOf(commitInfo1)
        modelService.invoker.addCommand(command1)

        actionService.undoLastAction()
        assertThat(modelService.invoker.commands.size).isEqualTo(0)
        assertThat(modelService.invoker.undoneCommands.size).isEqualTo(1)

        assertThat(modelService.branchInfo.currentCommits).isEqualTo(mutableListOf(commitInfo2, commitInfo1))
    }

    fun testRedoPick() {
        modelService.invoker.undoneCommands.clear()
        modelService.invoker.commands.clear()

        val command1 = SquashCommand(commitInfo1, mutableListOf(commitInfo2), "new message")
        val command2 = PickCommand(commitInfo1)

        commitInfo1.addChange(command1)
        commitInfo2.addChange(command1)

        modelService.branchInfo.currentCommits = mutableListOf(commitInfo1)
        modelService.invoker.addCommand(command1)
        modelService.invoker.undoneCommands.add(command2)

        actionService.redoLastAction()
        assertThat(modelService.invoker.commands.size).isEqualTo(2)
        assertThat(modelService.invoker.undoneCommands.size).isEqualTo(0)

        assertThat(modelService.branchInfo.currentCommits).isEqualTo(mutableListOf(commitInfo2, commitInfo1))
    }

    fun testRedoPickWithFixup() {
        modelService.invoker.undoneCommands.clear()
        modelService.invoker.commands.clear()

        val command1 = FixupCommand(commitInfo1, mutableListOf(commitInfo2))
        val command2 = PickCommand(commitInfo1)

        commitInfo1.addChange(command1)
        commitInfo2.addChange(command1)

        modelService.branchInfo.currentCommits = mutableListOf(commitInfo1)
        modelService.invoker.addCommand(command1)
        modelService.invoker.undoneCommands.add(command2)

        actionService.redoLastAction()
        assertThat(modelService.invoker.commands.size).isEqualTo(2)
        assertThat(modelService.invoker.undoneCommands.size).isEqualTo(0)

        assertThat(modelService.branchInfo.currentCommits).isEqualTo(mutableListOf(commitInfo2, commitInfo1))
    }

    fun testRedoFixup() {
        modelService.invoker.undoneCommands.clear()
        modelService.invoker.commands.clear()

        val command1 = FixupCommand(commitInfo1, mutableListOf(commitInfo2))

        modelService.branchInfo.currentCommits = mutableListOf(commitInfo2, commitInfo1)
        modelService.invoker.undoneCommands.add(command1)

        actionService.redoLastAction()
        assertThat(modelService.invoker.commands.size).isEqualTo(1)
        assertThat(modelService.invoker.undoneCommands.size).isEqualTo(0)

        assertThat(modelService.branchInfo.currentCommits).isEqualTo(mutableListOf(commitInfo1))
    }

    fun testRedoSquash() {
        modelService.invoker.undoneCommands.clear()
        modelService.invoker.commands.clear()

        val command1 = SquashCommand(commitInfo1, mutableListOf(commitInfo2), "new message")

        modelService.branchInfo.currentCommits = mutableListOf(commitInfo2, commitInfo1)
        modelService.invoker.undoneCommands.add(command1)

        actionService.redoLastAction()
        assertThat(modelService.invoker.commands.size).isEqualTo(1)
        assertThat(modelService.invoker.undoneCommands.size).isEqualTo(0)

        assertThat(modelService.branchInfo.currentCommits).isEqualTo(mutableListOf(commitInfo1))
    }

    fun testRemovePickFromSquashed() {
        modelService.invoker.undoneCommands.clear()
        modelService.invoker.commands.clear()
        modelService.invoker.commands.add(DropCommand(commitInfo1))
        actionService.removePickFromSquashed(listOf(commitInfo1, commitInfo2))
        assertThat(modelService.invoker.commands.size).isEqualTo(1)
    }

    fun testCheckCollapseLessThan7Commits() {
        val testEvent = createTestEvent()
        actionService.checkCollapse(testEvent)
        assertThat(testEvent.presentation.isEnabled).isFalse()
    }

    fun testCheckCollapseAlreadyCollapsed() {
        val commitProvider = TestGitCommitProvider(project)
        val commitInfo3 = CommitInfo(commitProvider.createCommit("bbb"), project, mutableListOf())
        val commitInfo4 = CommitInfo(commitProvider.createCommit("aaa"), project, mutableListOf())
        val commitInfo5 = CommitInfo(commitProvider.createCommit("ccc"), project, mutableListOf())
        val commitInfo6 = CommitInfo(commitProvider.createCommit("ddd"), project, mutableListOf())
        val commitInfo7 = CommitInfo(commitProvider.createCommit("fff"), project, mutableListOf())
        val commitInfo8 = CommitInfo(commitProvider.createCommit("ggg"), project, mutableListOf())

        modelService.branchInfo.initialCommits =
            mutableListOf(
                commitInfo1,
                commitInfo2,
                commitInfo3,
                commitInfo4,
                commitInfo5,
                commitInfo6,
                commitInfo7,
                commitInfo8,
            )
        modelService.branchInfo.currentCommits =
            mutableListOf(
                commitInfo1,
                commitInfo2,
                commitInfo3,
                commitInfo4,
                commitInfo5,
                commitInfo6,
                commitInfo7,
                commitInfo8,
            )
        commitInfo7.isCollapsed = true
        val testEvent = createTestEvent()
        actionService.checkCollapse(testEvent)
        assertThat(testEvent.presentation.isEnabled).isFalse()
    }

    fun testCheckCollapseAlreadyCollapsedNoAddedBranch() {
        modelService.graphInfo.addedBranch = null
        val commitProvider = TestGitCommitProvider(project)
        val commitInfo3 = CommitInfo(commitProvider.createCommit("bbb"), project, mutableListOf())
        val commitInfo4 = CommitInfo(commitProvider.createCommit("aaa"), project, mutableListOf())
        val commitInfo5 = CommitInfo(commitProvider.createCommit("ccc"), project, mutableListOf())
        val commitInfo6 = CommitInfo(commitProvider.createCommit("ddd"), project, mutableListOf())
        val commitInfo7 = CommitInfo(commitProvider.createCommit("fff"), project, mutableListOf())
        val commitInfo8 = CommitInfo(commitProvider.createCommit("ggg"), project, mutableListOf())

        modelService.branchInfo.initialCommits =
            mutableListOf(
                commitInfo1,
                commitInfo2,
                commitInfo3,
                commitInfo4,
                commitInfo5,
                commitInfo6,
                commitInfo7,
                commitInfo8,
            )
        modelService.branchInfo.currentCommits =
            mutableListOf(
                commitInfo1,
                commitInfo2,
                commitInfo3,
                commitInfo4,
                commitInfo5,
                commitInfo6,
                commitInfo7,
                commitInfo8,
            )
        commitInfo7.isCollapsed = true
        val testEvent = createTestEvent()
        actionService.checkCollapse(testEvent)
        assertThat(testEvent.presentation.isEnabled).isFalse()
    }

    fun testCheckCollapseAlreadyCollapsedWithSecondBranch() {
        val commitProvider = TestGitCommitProvider(project)
        val commitInfo7 = CommitInfo(commitProvider.createCommit("bbb"), project, mutableListOf())
        val commitInfo8 = CommitInfo(commitProvider.createCommit("aaa"), project, mutableListOf())
        val commitInfo9 = CommitInfo(commitProvider.createCommit("ggg"), project, mutableListOf())
        val commitInfo10 = CommitInfo(commitProvider.createCommit("ccc"), project, mutableListOf())
        val commitInfo11 = CommitInfo(commitProvider.createCommit("ddd"), project, mutableListOf())
        val commitInfo12 = CommitInfo(commitProvider.createCommit("fff"), project, mutableListOf())
        val commitInfo13 = CommitInfo(commitProvider.createCommit("eee"), project, mutableListOf())
        val commitInfo14 = CommitInfo(commitProvider.createCommit("jjj"), project, mutableListOf())

        modelService.branchInfo.initialCommits =
            mutableListOf(
                commitInfo1,
                commitInfo2,
                commitInfo6,
                commitInfo7,
                commitInfo8,
                commitInfo9,
                commitInfo13,
                commitInfo14,
            )
        modelService.branchInfo.currentCommits = modelService.branchInfo.initialCommits.toMutableList()
        addedBranch.currentCommits =
            mutableListOf(
                commitInfo3,
                commitInfo4,
                commitInfo5,
                commitInfo10,
                commitInfo11,
                commitInfo12,
            )
        addedBranch.initialCommits = addedBranch.currentCommits
        commitInfo7.isCollapsed = true
        commitInfo5.isCollapsed = true
        val testEvent = createTestEvent()
        actionService.checkCollapse(testEvent)
        assertThat(testEvent.presentation.isEnabled).isFalse()
    }

    fun testCheckCollapseOnly1Selected() {
        val commitProvider = TestGitCommitProvider(project)
        val commitInfo3 = CommitInfo(commitProvider.createCommit("bbb"), project, mutableListOf())
        val commitInfo4 = CommitInfo(commitProvider.createCommit("aaa"), project, mutableListOf())
        val commitInfo5 = CommitInfo(commitProvider.createCommit("ccc"), project, mutableListOf())
        val commitInfo6 = CommitInfo(commitProvider.createCommit("ddd"), project, mutableListOf())
        val commitInfo7 = CommitInfo(commitProvider.createCommit("fff"), project, mutableListOf())
        val commitInfo8 = CommitInfo(commitProvider.createCommit("ggg"), project, mutableListOf())

        modelService.branchInfo.initialCommits =
            mutableListOf(
                commitInfo1,
                commitInfo2,
                commitInfo3,
                commitInfo4,
                commitInfo5,
                commitInfo6,
                commitInfo7,
                commitInfo8,
            )
        modelService.branchInfo.currentCommits =
            mutableListOf(
                commitInfo1,
                commitInfo2,
                commitInfo3,
                commitInfo4,
                commitInfo5,
                commitInfo6,
                commitInfo7,
                commitInfo8,
            )
        for (commit in modelService.branchInfo.currentCommits) {
            commit.isCollapsed = false
        }
        modelService.branchInfo.clearSelectedCommits()
        modelService.addToSelectedCommits(commitInfo1, modelService.branchInfo)
        val testEvent = createTestEvent()
        actionService.checkCollapse(testEvent)
        assertThat(testEvent.presentation.isEnabled).isFalse()
    }

    fun testCheckCollapseNoSelectedCommits() {
        val commitProvider = TestGitCommitProvider(project)
        val commitInfo3 = CommitInfo(commitProvider.createCommit("bbb"), project, mutableListOf())
        val commitInfo4 = CommitInfo(commitProvider.createCommit("aaa"), project, mutableListOf())
        val commitInfo5 = CommitInfo(commitProvider.createCommit("ccc"), project, mutableListOf())
        val commitInfo6 = CommitInfo(commitProvider.createCommit("ddd"), project, mutableListOf())
        val commitInfo7 = CommitInfo(commitProvider.createCommit("fff"), project, mutableListOf())
        val commitInfo8 = CommitInfo(commitProvider.createCommit("ggg"), project, mutableListOf())

        modelService.branchInfo.initialCommits =
            mutableListOf(
                commitInfo1,
                commitInfo2,
                commitInfo3,
                commitInfo4,
                commitInfo5,
                commitInfo6,
                commitInfo7,
                commitInfo8,
            )
        modelService.branchInfo.currentCommits =
            mutableListOf(
                commitInfo1,
                commitInfo2,
                commitInfo3,
                commitInfo4,
                commitInfo5,
                commitInfo6,
                commitInfo7,
                commitInfo8,
            )
        for (commit in modelService.branchInfo.currentCommits) {
            commit.isCollapsed = false
        }
        modelService.branchInfo.clearSelectedCommits()
        val testEvent = createTestEvent()
        actionService.checkCollapse(testEvent)
        assertThat(testEvent.presentation.isEnabled).isTrue()
    }

    fun testCheckCollapseCommitsAreInARange() {
        val commitProvider = TestGitCommitProvider(project)
        val commitInfo3 = CommitInfo(commitProvider.createCommit("bbb"), project, mutableListOf())
        val commitInfo4 = CommitInfo(commitProvider.createCommit("aaa"), project, mutableListOf())
        val commitInfo5 = CommitInfo(commitProvider.createCommit("ccc"), project, mutableListOf())
        val commitInfo6 = CommitInfo(commitProvider.createCommit("ddd"), project, mutableListOf())
        val commitInfo7 = CommitInfo(commitProvider.createCommit("fff"), project, mutableListOf())
        val commitInfo8 = CommitInfo(commitProvider.createCommit("ggg"), project, mutableListOf())

        modelService.branchInfo.initialCommits =
            mutableListOf(
                commitInfo1,
                commitInfo2,
                commitInfo3,
                commitInfo4,
                commitInfo5,
                commitInfo6,
                commitInfo7,
                commitInfo8,
            )
        modelService.branchInfo.currentCommits =
            mutableListOf(
                commitInfo1,
                commitInfo2,
                commitInfo3,
                commitInfo4,
                commitInfo5,
                commitInfo6,
                commitInfo7,
                commitInfo8,
            )
        for (commit in modelService.branchInfo.currentCommits) {
            commit.isCollapsed = false
        }
        modelService.branchInfo.clearSelectedCommits()
        modelService.addToSelectedCommits(commitInfo1, modelService.branchInfo)
        modelService.addToSelectedCommits(commitInfo2, modelService.branchInfo)
        val testEvent = createTestEvent()
        actionService.checkCollapse(testEvent)
        assertThat(testEvent.presentation.isEnabled).isTrue()
    }

    fun testCheckCollapseCommitsAreNotInARange() {
        val commitProvider = TestGitCommitProvider(project)
        val commitInfo3 = CommitInfo(commitProvider.createCommit("bbb"), project, mutableListOf())
        val commitInfo4 = CommitInfo(commitProvider.createCommit("aaa"), project, mutableListOf())
        val commitInfo5 = CommitInfo(commitProvider.createCommit("ccc"), project, mutableListOf())
        val commitInfo6 = CommitInfo(commitProvider.createCommit("ddd"), project, mutableListOf())
        val commitInfo7 = CommitInfo(commitProvider.createCommit("fff"), project, mutableListOf())
        val commitInfo8 = CommitInfo(commitProvider.createCommit("ggg"), project, mutableListOf())

        modelService.branchInfo.initialCommits =
            mutableListOf(
                commitInfo1,
                commitInfo2,
                commitInfo3,
                commitInfo4,
                commitInfo5,
                commitInfo6,
                commitInfo7,
                commitInfo8,
            )
        modelService.branchInfo.currentCommits =
            mutableListOf(
                commitInfo1,
                commitInfo2,
                commitInfo3,
                commitInfo4,
                commitInfo5,
                commitInfo6,
                commitInfo7,
                commitInfo8,
            )
        for (commit in modelService.branchInfo.currentCommits) {
            commit.isCollapsed = false
        }
        modelService.branchInfo.clearSelectedCommits()

        modelService.addToSelectedCommits(commitInfo1, modelService.branchInfo)
        modelService.addToSelectedCommits(commitInfo5, modelService.branchInfo)

        val testEvent = createTestEvent()
        actionService.checkCollapse(testEvent)
        assertThat(testEvent.presentation.isEnabled).isFalse()
    }

    fun testCheckValidParentWhenSquashing() {
        val commitProvider = TestGitCommitProvider(project)
        val commitInfo3 = CommitInfo(commitProvider.createCommit("bbb"), project, mutableListOf())
        val commitInfo4 = CommitInfo(commitProvider.createCommit("aaa"), project, mutableListOf())
        val commitInfo5 = CommitInfo(commitProvider.createCommit("ccc"), project, mutableListOf())
        val commitInfo6 = CommitInfo(commitProvider.createCommit("ddd"), project, mutableListOf())
        val commitInfo7 = CommitInfo(commitProvider.createCommit("fff"), project, mutableListOf())
        val commitInfo8 = CommitInfo(commitProvider.createCommit("ggg"), project, mutableListOf())

        modelService.branchInfo.initialCommits =
            mutableListOf(
                commitInfo1,
                commitInfo2,
                commitInfo3,
                commitInfo4,
                commitInfo5,
                commitInfo6,
                commitInfo7,
                commitInfo8,
            )
        modelService.branchInfo.currentCommits =
            mutableListOf(
                commitInfo1,
                commitInfo2,
                commitInfo3,
                commitInfo4,
                commitInfo5,
                commitInfo6,
                commitInfo7,
                commitInfo8,
            )
        for (commit in modelService.branchInfo.currentCommits) {
            commit.isCollapsed = false
        }
        modelService.branchInfo.clearSelectedCommits()

        modelService.addToSelectedCommits(commitInfo1, modelService.branchInfo)
        commitInfo2.isCollapsed = true

        val testEvent = createTestEvent()
        actionService.checkFixupOrSquash(testEvent)
        assertThat(testEvent.presentation.isEnabled).isFalse()
    }

    fun testCheckValidParentWhenSquashingIs() {
        val commitProvider = TestGitCommitProvider(project)
        val commitInfo3 = CommitInfo(commitProvider.createCommit("bbb"), project, mutableListOf())
        val commitInfo4 = CommitInfo(commitProvider.createCommit("aaa"), project, mutableListOf())
        val commitInfo5 = CommitInfo(commitProvider.createCommit("ccc"), project, mutableListOf())
        val commitInfo6 = CommitInfo(commitProvider.createCommit("ddd"), project, mutableListOf())
        val commitInfo7 = CommitInfo(commitProvider.createCommit("fff"), project, mutableListOf())
        val commitInfo8 = CommitInfo(commitProvider.createCommit("ggg"), project, mutableListOf())

        modelService.branchInfo.initialCommits =
            mutableListOf(
                commitInfo1,
                commitInfo2,
                commitInfo3,
                commitInfo4,
                commitInfo5,
                commitInfo6,
                commitInfo7,
                commitInfo8,
            )
        modelService.branchInfo.currentCommits =
            mutableListOf(
                commitInfo1,
                commitInfo2,
                commitInfo3,
                commitInfo4,
                commitInfo5,
                commitInfo6,
                commitInfo7,
                commitInfo8,
            )
        for (commit in modelService.branchInfo.currentCommits) {
            commit.isCollapsed = false
        }
        modelService.branchInfo.clearSelectedCommits()

        modelService.addToSelectedCommits(commitInfo1, modelService.branchInfo)

        val testEvent = createTestEvent()
        actionService.checkFixupOrSquash(testEvent)
        assertThat(testEvent.presentation.isEnabled).isTrue()
    }

    fun testExpandCommitsNotAlreadyCollapsed() {
        commitInfo1.isCollapsed = false
        actionService.expandCollapsedCommits(commitInfo1, branchInfo)
        assertThat(commitInfo1.isCollapsed).isFalse()
    }

    fun testExpandCommitsAlreadyCollapsed() {
        val commitProvider = TestGitCommitProvider(project)
        val commitInfo3 = CommitInfo(commitProvider.createCommit("bbb"), project, mutableListOf())
        val commitInfo4 = CommitInfo(commitProvider.createCommit("aaa"), project, mutableListOf())
        val commitInfo5 = CommitInfo(commitProvider.createCommit("ccc"), project, mutableListOf())
        val commitInfo6 = CommitInfo(commitProvider.createCommit("ddd"), project, mutableListOf())
        val commitInfo7 = CommitInfo(commitProvider.createCommit("fff"), project, mutableListOf())
        val commitInfo8 = CommitInfo(commitProvider.createCommit("ggg"), project, mutableListOf())

        modelService.branchInfo.initialCommits =
            mutableListOf(
                commitInfo1,
                commitInfo2,
                commitInfo3,
                commitInfo4,
                commitInfo5,
                commitInfo6,
                commitInfo7,
                commitInfo8,
            )
        modelService.branchInfo.currentCommits =
            mutableListOf(
                commitInfo1,
                commitInfo2,
                commitInfo3,
                commitInfo4,
                commitInfo5,
                commitInfo6,
                commitInfo7,
                commitInfo8,
            )

        commitInfo7.isCollapsed = true
        commitInfo6.isCollapsed = true
        commitInfo5.isCollapsed = true
        modelService.branchInfo.currentCommits = mutableListOf(commitInfo1, commitInfo2, commitInfo3, commitInfo4, commitInfo7, commitInfo8)
        val collapseCommand = CollapseCommand(commitInfo7, mutableListOf(commitInfo5, commitInfo6))
        commitInfo7.addChange(collapseCommand)

        actionService.expandCollapsedCommits(commitInfo7, branchInfo)
        assertThat(commitInfo7.isCollapsed).isFalse()
        assertThat(commitInfo6.isCollapsed).isFalse()
        assertThat(commitInfo5.isCollapsed).isFalse()

        assertThat(commitInfo7.changes.size).isEqualTo(0)
        assertThat(
            modelService.branchInfo.currentCommits,
        ).isEqualTo(mutableListOf(commitInfo1, commitInfo2, commitInfo3, commitInfo4, commitInfo5, commitInfo6, commitInfo7, commitInfo8))
    }

    fun testTakeCollapseAction() {
        val commitProvider = TestGitCommitProvider(project)
        val commitInfo3 = CommitInfo(commitProvider.createCommit("bbb"), project, mutableListOf())
        val commitInfo4 = CommitInfo(commitProvider.createCommit("aaa"), project, mutableListOf())
        val commitInfo5 = CommitInfo(commitProvider.createCommit("ccc"), project, mutableListOf())
        val commitInfo6 = CommitInfo(commitProvider.createCommit("ddd"), project, mutableListOf())
        val commitInfo7 = CommitInfo(commitProvider.createCommit("fff"), project, mutableListOf())
        val commitInfo8 = CommitInfo(commitProvider.createCommit("ggg"), project, mutableListOf())

        modelService.branchInfo.initialCommits =
            mutableListOf(
                commitInfo1,
                commitInfo2,
                commitInfo3,
                commitInfo4,
                commitInfo5,
                commitInfo6,
                commitInfo7,
                commitInfo8,
            )
        modelService.branchInfo.currentCommits =
            mutableListOf(
                commitInfo1,
                commitInfo2,
                commitInfo3,
                commitInfo4,
                commitInfo5,
                commitInfo6,
                commitInfo7,
                commitInfo8,
            )
        modelService.branchInfo.clearSelectedCommits()

        actionService.takeCollapseAction()
        assertThat(commitInfo7.isCollapsed).isTrue()
        assertThat(
            modelService.branchInfo.currentCommits,
        ).isEqualTo(mutableListOf(commitInfo1, commitInfo2, commitInfo3, commitInfo4, commitInfo5, commitInfo7, commitInfo8))
        assertThat(commitInfo7.changes.size).isEqualTo(1)
    }

    fun testTaleCollapseActionWithSelectedCommits() {
        val commitProvider = TestGitCommitProvider(project)
        val commitInfo3 = CommitInfo(commitProvider.createCommit("bbb"), project, mutableListOf())
        val commitInfo4 = CommitInfo(commitProvider.createCommit("aaa"), project, mutableListOf())
        val commitInfo5 = CommitInfo(commitProvider.createCommit("ccc"), project, mutableListOf())
        val commitInfo6 = CommitInfo(commitProvider.createCommit("ddd"), project, mutableListOf())
        val commitInfo7 = CommitInfo(commitProvider.createCommit("fff"), project, mutableListOf())
        val commitInfo8 = CommitInfo(commitProvider.createCommit("ggg"), project, mutableListOf())

        modelService.branchInfo.initialCommits =
            mutableListOf(
                commitInfo1,
                commitInfo2,
                commitInfo3,
                commitInfo4,
                commitInfo5,
                commitInfo6,
                commitInfo7,
                commitInfo8,
            )
        modelService.branchInfo.currentCommits =
            mutableListOf(
                commitInfo1,
                commitInfo2,
                commitInfo3,
                commitInfo4,
                commitInfo5,
                commitInfo6,
                commitInfo7,
                commitInfo8,
            )
        modelService.branchInfo.clearSelectedCommits()
        modelService.addToSelectedCommits(commitInfo1, modelService.branchInfo)
        modelService.addToSelectedCommits(commitInfo2, modelService.branchInfo)
        actionService.takeCollapseAction()
        assertThat(commitInfo2.isCollapsed).isTrue()
        assertThat(
            modelService.branchInfo.currentCommits,
        ).isEqualTo(mutableListOf(commitInfo2, commitInfo3, commitInfo4, commitInfo5, commitInfo6, commitInfo7, commitInfo8))
        assertThat(commitInfo2.changes.size).isEqualTo(1)
    }

    fun testTakeCherryPickAction() {
        modelService.invoker.commands.clear()
        modelService.addToSelectedCommits(commitInfo3, addedBranch)
        actionService.takeCherryPickAction()
        assertThat(branchInfo.selectedCommits.isEmpty()).isTrue()
        assertThat(addedBranch.selectedCommits.isEmpty()).isTrue()
        assertTrue(branchInfo.currentCommits.size == 4)
        assertTrue(branchInfo.currentCommits[0].changes.any { it is CherryCommand })
        modelService.graphInfo.addedBranch = null
        actionService.takeCherryPickAction()
        assertThat(project.service<RebaseInvoker>().commands.size).isEqualTo(1)
    }

    fun testCheckCherryPickAction() {
        modelService.addToSelectedCommits(commitInfo4, addedBranch)
        modelService.invoker.commands.clear()
        val testEvent = createTestEvent()
        actionService.checkCherryPick(testEvent)
        assertThat(testEvent.presentation.isEnabled).isFalse()
        branchInfo.selectedCommits.clear()
        actionService.checkCherryPick(testEvent)
        assertThat(testEvent.presentation.isEnabled).isTrue()
        addedBranch.selectedCommits.add(commitInfo5)
        commitInfo5.wasCherryPicked = true
        actionService.checkCherryPick(testEvent)
        assertThat(testEvent.presentation.isEnabled).isFalse()
        addedBranch.selectedCommits.add(commitInfo5)
        commitInfo5.wasCherryPicked = false
        actionService.checkCherryPick(testEvent)
        assertThat(testEvent.presentation.isEnabled).isTrue()
        modelService.graphInfo.addedBranch = null
        actionService.checkCherryPick(testEvent)
        assertThat(testEvent.presentation.isEnabled).isFalse()
    }

    fun testNormalRebaseAction() {
    }

    fun testRebasingCommandCreation() {
        modelService.invoker.commands.clear()
        modelService.addToSelectedCommits(commitInfo3, addedBranch)
        actionService.takeNormalRebaseAction()
        assertThat(branchInfo.selectedCommits.isEmpty()).isTrue()
        assertThat(addedBranch.selectedCommits.isEmpty()).isTrue()
        assertTrue(project.service<RebaseInvoker>().commands.isNotEmpty())
        modelService.graphInfo.addedBranch = null
        actionService.takeNormalRebaseAction()
        assertThat(project.service<RebaseInvoker>().commands.size).isEqualTo(1)
    }

    fun testCheckNormalRebaseAction() {
        val testEvent = createTestEvent()
        actionService.checkNormalRebaseAction(testEvent)
        assertThat(testEvent.presentation.isEnabled).isTrue()
        modelService.addToSelectedCommits(commitInfo4, addedBranch)
        actionService.checkNormalRebaseAction(testEvent)
        assertThat(testEvent.presentation.isEnabled).isTrue()
        modelService.addToSelectedCommits(commitInfo5, addedBranch)
        actionService.checkNormalRebaseAction(testEvent)
        assertThat(testEvent.presentation.isEnabled).isFalse()
        addedBranch.clearSelectedCommits()
        modelService.addToSelectedCommits(commitInfo3, addedBranch)
        actionService.checkNormalRebaseAction(testEvent)
        assertThat(testEvent.presentation.isEnabled).isFalse()

        modelService.graphInfo.addedBranch = null
        actionService.checkNormalRebaseAction(testEvent)
        assertThat(testEvent.presentation.isEnabled).isFalse()

        addedBranch.clearSelectedCommits()
        modelService.addToSelectedCommits(commitInfo5, addedBranch)
        actionService.checkNormalRebaseAction(testEvent)
        assertThat(testEvent.presentation.isEnabled).isFalse()
    }

    fun testGetParentWithoutDrop() {
        modelService.addToSelectedCommits(commitInfo2, branchInfo)
        actionService.takeDropAction()
        assertThat(branchInfo.selectedCommits.isEmpty()).isTrue()
        assertThat(addedBranch.selectedCommits.isEmpty()).isTrue()
        assertTrue(project.service<RebaseInvoker>().commands.isNotEmpty())
        modelService.addToSelectedCommits(commitInfo1, branchInfo)
        actionService.getParent()
        assertThat(actionService.getParent()).isEqualTo(commitInfo6)
    }

    fun testUndoRebase() {
        modelService.invoker.undoneCommands.clear()
        modelService.invoker.commands.clear()
        val command1 = RebaseCommand(commitInfo4)
        val command2 = RebaseCommand(commitInfo5)
        branchInfo.isRebased = true

        // modelService.branchInfo.currentCommits = mutableListOf(commitInfo2, commitInfo1)
        modelService.invoker.addCommand(command1)
        modelService.invoker.addCommand(command2)
        actionService.undoLastAction()
        assertThat(modelService.invoker.commands.size).isEqualTo(1)
        assertThat(modelService.invoker.undoneCommands.size).isEqualTo(1)
        assertThat(addedBranch.baseCommit).isEqualTo(commitInfo4)
        assertFalse(modelService.graphInfo.mainBranch.isRebased)
    }

    fun testRedoRebase() {
        modelService.invoker.undoneCommands.clear()
        modelService.invoker.commands.clear()
        val command1 = RebaseCommand(commitInfo4)
        val command2 = RebaseCommand(commitInfo5)
        branchInfo.isRebased = true

        modelService.invoker.addCommand(command1)
        modelService.invoker.undoneCommands.add(command2)
        actionService.redoLastAction()
        assertThat(modelService.invoker.commands.size).isEqualTo(2)
        assertThat(modelService.invoker.undoneCommands.size).isEqualTo(0)
        assertThat(addedBranch.baseCommit).isEqualTo(commitInfo5)
        assertTrue(branchInfo.isRebased)
    }

    fun testPrepareCherry() {
        modelService.invoker.undoneCommands.clear()
        modelService.invoker.commands.clear()
        actionService.prepareCherry(commitInfo4, 0)
        assertThat(modelService.invoker.commands.size).isEqualTo(1)
        assertThat(branchInfo.currentCommits.size).isEqualTo(4)
        assertTrue(commitInfo4.wasCherryPicked)
    }

    fun testUndoCherry() {
        modelService.invoker.undoneCommands.clear()
        modelService.invoker.commands.clear()
        actionService.prepareCherry(commitInfo4, 0)
        actionService.undoLastAction()
        assertThat(modelService.invoker.commands.size).isEqualTo(0)
        assertThat(modelService.invoker.undoneCommands.size).isEqualTo(1)
        assertThat(branchInfo.currentCommits.size).isEqualTo(3)
        assertFalse(commitInfo4.wasCherryPicked)
        actionService.redoLastAction()
        commitInfo4.wasCherryPicked = false
        actionService.undoLastAction()
        assertFalse(commitInfo4.wasCherryPicked)
        actionService.redoLastAction()
        modelService.graphInfo.addedBranch = null
        actionService.undoLastAction()
        assertTrue(commitInfo4.wasCherryPicked)
    }

    fun testRedoCherry() {
        modelService.invoker.undoneCommands.clear()
        modelService.invoker.commands.clear()
        actionService.prepareCherry(commitInfo4, 0)
        actionService.undoLastAction()
        actionService.redoLastAction()
        assertThat(modelService.invoker.commands.size).isEqualTo(1)
        assertThat(modelService.invoker.undoneCommands.size).isEqualTo(0)
        assertThat(branchInfo.currentCommits.size).isEqualTo(4)
        assertTrue(commitInfo4.wasCherryPicked)
    }

    fun testCollapseAgain() {
        val alreadyCollapsed: MutableList<CommitInfo> = mutableListOf()
        val commitProvider = TestGitCommitProvider(project)
        for (i in 1..30) {
            alreadyCollapsed.add(CommitInfo(commitProvider.createCommit("$i"), project))
        }
        modelService.branchInfo.currentCommits = mutableListOf(commitInfo1)
        modelService.branchInfo.initialCommits = alreadyCollapsed
        val command = CollapseCommand(commitInfo1, alreadyCollapsed)

        alreadyCollapsed.forEach {
            it.changes.add(command)
            it.isCollapsed = true
        }
        commitInfo1.changes.add(command)
        commitInfo1.isCollapsed = true
        actionService.expandCollapsedCommits(commitInfo1, modelService.branchInfo)

        assertThat(modelService.branchInfo.currentCommits).hasSize(21)
        val expectedCommand = CollapseCommand(commitInfo1, alreadyCollapsed.drop(20).toMutableList())
        assertThat(commitInfo1.changes).contains(expectedCommand)
        assertThat(commitInfo1.isCollapsed).isTrue()
        for (i in 20..29) {
            assertThat(alreadyCollapsed[i].isCollapsed).isTrue()
            assertThat(alreadyCollapsed[i].changes).containsExactly(expectedCommand)
        }
        for (i in 0..19) {
            assertThat(alreadyCollapsed[i].isCollapsed).isFalse()
            assertThat(alreadyCollapsed[i].changes).doesNotContain(expectedCommand)
            assertThat(modelService.branchInfo.currentCommits).contains(alreadyCollapsed[i])
        }
    }

    fun test() {
        val action = RewordAction()
        val testev = createTestEvent(action)
        val bbut = action.createCustomComponent(testev.presentation, "string")
        assertThat(bbut).isNotNull()
    }

    private inline fun <reified T> anyCustom(): T = ArgumentMatchers.any(T::class.java)
}
