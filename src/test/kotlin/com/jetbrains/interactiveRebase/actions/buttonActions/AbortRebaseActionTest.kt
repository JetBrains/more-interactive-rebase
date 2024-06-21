package com.jetbrains.interactiveRebase.actions.buttonActions

import com.intellij.icons.ExpUiIcons
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

class AbortRebaseActionTest : BasePlatformTestCase() {
    private lateinit var action: AbortRebaseAction

    override fun setUp() {
        super.setUp()
        action = AbortRebaseAction()

    }

    fun testActionUpdate() {
        val testEvent = TestActionEvent.createTestEvent()
        action.update(testEvent)
        assertThat(testEvent.presentation.icon).isEqualTo(ExpUiIcons.Vcs.Abort)
    }

    private inline fun <reified T> anyCustom(): T = ArgumentMatchers.any(T::class.java)
}
