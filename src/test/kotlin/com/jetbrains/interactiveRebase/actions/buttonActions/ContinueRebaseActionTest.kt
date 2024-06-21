package com.jetbrains.interactiveRebase.actions.buttonActions

import com.intellij.icons.ExpUiIcons
import com.intellij.testFramework.TestActionEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.assertj.core.api.Assertions.assertThat
import org.mockito.ArgumentMatchers

class ContinueRebaseActionTest : BasePlatformTestCase() {
    private lateinit var action: ContinueRebaseAction

    override fun setUp() {
        super.setUp()
        action = ContinueRebaseAction()
    }

    fun testActionUpdate() {
        val testEvent = TestActionEvent.createTestEvent()
        action.update(testEvent)
        assertThat(testEvent.presentation.icon).isEqualTo(ExpUiIcons.Vcs.ResolveContinue)
    }

    private inline fun <reified T> anyCustom(): T = ArgumentMatchers.any(T::class.java)
}
