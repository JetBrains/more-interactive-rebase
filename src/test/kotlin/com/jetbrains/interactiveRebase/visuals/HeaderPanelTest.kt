package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.actions.gitPanel.RebaseActionsGroup
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import javax.swing.JButton
import javax.swing.JComponent

class HeaderPanelTest : BasePlatformTestCase() {
    private lateinit var headerPanel: HeaderPanel
    private lateinit var actionManager: ActionManager
    private lateinit var rebaseGroup: RebaseActionsGroup
    private lateinit var actionToolbar: ActionToolbar
    private var toolBarComponent: JBPanel<JBPanel<*>> = JBPanel()

    override fun setUp() {
        super.setUp()
        actionManager = mock(ActionManager::class.java)
        rebaseGroup = RebaseActionsGroup()
        actionToolbar = mock(ActionToolbar::class.java)
        `when`(actionToolbar.component).thenReturn(toolBarComponent)
        `when`(actionManager.getAction(anyCustom())).thenReturn(rebaseGroup)
        `when`(actionManager.createActionToolbar(anyCustom(), anyCustom(), anyCustom())).thenReturn(actionToolbar)
        headerPanel = HeaderPanel(project, actionManager)
    }

    fun testIncludesGitButtons() {
        val captorGroup = ArgumentCaptor.forClass(RebaseActionsGroup::class.java)
        val captorPlaces = ArgumentCaptor.forClass(String::class.java)
        val captorHorizontal = ArgumentCaptor.forClass(Boolean::class.java)
        verify(actionManager, times(2)).createActionToolbar(captorPlaces.capture(), captorGroup.capture(), captorHorizontal.capture())
        assertThat(captorPlaces.value).isEqualTo(ActionPlaces.EDITOR_TAB)
        assertThat(captorHorizontal.value).isTrue()
    }


    private inline fun <reified T> anyCustom(): T = any(T::class.java)
}
