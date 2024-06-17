package com.jetbrains.interactiveRebase.actions.gitPanel

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.application.EDT
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.GraphInfo
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.visuals.CommitInfoPanel
import com.jetbrains.interactiveRebase.visuals.GraphPanel
import com.jetbrains.interactiveRebase.visuals.MainPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.mockito.ArgumentMatchers
import org.mockito.Mockito

class FixupActionTest : BasePlatformTestCase() {
    private lateinit var action: FixupAction
    private lateinit var actionService: ActionService
    private lateinit var mainPanel: MainPanel
    private lateinit var modelService: ModelService
    private lateinit var commitInfo1: CommitInfo
    private lateinit var commitInfo2: CommitInfo
    private lateinit var commitInfo3: CommitInfo
    private lateinit var branchInfo: BranchInfo
    private var addedBranch: BranchInfo = BranchInfo()

    override fun setUp() {
        super.setUp()
        action = FixupAction()
        val commitProvider = TestGitCommitProvider(project)
        commitInfo1 = CommitInfo(commitProvider.createCommit("tests"), project, mutableListOf())
        commitInfo2 = CommitInfo(commitProvider.createCommit("fix tests"), project, mutableListOf())
        commitInfo3 = CommitInfo(commitProvider.createCommit("belongs to added branch"), project, mutableListOf())
        val commitService = Mockito.mock(CommitService::class.java)

        Mockito.doAnswer {
            listOf(commitInfo1.commit)
        }.`when`(commitService).getCommits(ArgumentMatchers.anyString())

        Mockito.doAnswer {
            "feature1"
        }.`when`(commitService).getBranchName()

        modelService = ModelService(project, CoroutineScope(Dispatchers.EDT), commitService)
        modelService.branchInfo.initialCommits = mutableListOf(commitInfo1, commitInfo2)
        modelService.branchInfo.currentCommits = mutableListOf(commitInfo1, commitInfo2)
        addedBranch.name = "added"
        addedBranch.currentCommits.add(commitInfo3)
        addedBranch.baseCommit = commitInfo3
        modelService.graphInfo = GraphInfo(modelService.branchInfo, addedBranch)
        modelService.addToSelectedCommits(commitInfo1, modelService.branchInfo)

        modelService.branchInfo.setName("feature1")
        modelService.invoker.branchInfo = modelService.branchInfo

        branchInfo = modelService.branchInfo
        mainPanel = MainPanel(project)
        mainPanel.commitInfoPanel = Mockito.mock(CommitInfoPanel::class.java)
        mainPanel.graphPanel = GraphPanel(project, modelService.graphInfo)
        Mockito.doNothing().`when`(mainPanel.commitInfoPanel).commitsSelected(anyCustom())
        Mockito.doNothing().`when`(mainPanel.commitInfoPanel).repaint()
        actionService = ActionService(project, modelService, modelService.invoker)
        actionService.mainPanel = mainPanel
    }

    fun testGetActionUpdateThread() {
        assertThat(action.getActionUpdateThread()).isEqualTo(ActionUpdateThread.EDT)
    }

    fun testActionDisabled() {
        val event = TestActionEvent.createTestEvent()
        commitInfo1.isSelected = false
        modelService.branchInfo.clearSelectedCommits()
        action.update(event)
        assertThat(event.presentation.isEnabled).isFalse()
        modelService.branchInfo.addSelectedCommits(commitInfo2)
        action.update(event)
        assertThat(event.presentation.isEnabled).isFalse()
    }

    private inline fun <reified T> anyCustom(): T = ArgumentMatchers.any(T::class.java)
}
