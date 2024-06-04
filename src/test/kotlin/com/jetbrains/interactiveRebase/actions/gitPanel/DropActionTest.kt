package com.jetbrains.interactiveRebase.actions.gitPanel

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.EDT
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.mockStructs.MockGitRepository
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.CommitService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.utils.gitUtils.IRGitUtils
import com.jetbrains.interactiveRebase.visuals.CommitInfoPanel
import com.jetbrains.interactiveRebase.visuals.MainPanel
import com.jetbrains.rd.swing.mouseOrKeyReleased
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import java.awt.event.InputEvent
import java.awt.event.MouseEvent

class DropActionTest : BasePlatformTestCase() {

    private lateinit var mainPanel: MainPanel
    private lateinit var modelService: ModelService
    private lateinit var commitInfo1: CommitInfo
    private lateinit var commitInfo2: CommitInfo
    private lateinit var branchInfo: BranchInfo
    private lateinit var actionService: ActionService
    private lateinit var dropAction: RewordAction
    private lateinit var e: AnActionEvent

    override fun setUp() {
        dropAction = RewordAction()
        super.setUp()
        val commitProvider = TestGitCommitProvider(project)
        val repo = MockGitRepository("branch")
        commitInfo1 = CommitInfo(commitProvider.createCommit("tests"), project, mutableListOf())
        commitInfo2 = CommitInfo(commitProvider.createCommit("fix tests"), project, mutableListOf())
        val commitService = Mockito.mock(CommitService::class.java)
        val gitUtils = Mockito.mock(IRGitUtils::class.java)

        Mockito.doAnswer {
            repo
        }.`when`(gitUtils).getRepository()

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
        mainPanel.commitInfoPanel = Mockito.mock(CommitInfoPanel::class.java)
        Mockito.doNothing().`when`(mainPanel.commitInfoPanel).commitsSelected(anyCustom())
        Mockito.doNothing().`when`(mainPanel.commitInfoPanel).repaint()

        actionService = ActionService(project, modelService, modelService.invoker)
        e = Mockito.mock(AnActionEvent::class.java)
        Mockito.doAnswer {
            project
        }.`when`(e).project



    }


    fun testTakeRewordAction() {
        dropAction.actionPerformed(e)
        Assertions.assertThat(commitInfo1.isTextFieldEnabled).isTrue()
        Assertions.assertThat(commitInfo2.isTextFieldEnabled).isFalse()
    }

    private inline fun <reified T> anyCustom(): T = ArgumentMatchers.any(T::class.java)
}