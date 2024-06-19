package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.jetbrains.interactiveRebase.actions.gitPanel.RebaseActionsGroup
import com.jetbrains.interactiveRebase.services.ModelService
import java.awt.BorderLayout
import java.awt.Graphics
import javax.swing.BoxLayout
import javax.swing.JComponent

class HeaderPanel(private val project: Project, private val actionManager: ActionManager = ActionManager.getInstance()) :
    JBPanel<JBPanel<*>>() {
    private val gitActionsPanel = JBPanel<JBPanel<*>>()
    val changeActionsPanel = JBPanel<JBPanel<*>>()
    val rebaseProcessPanel = JBPanel<JBPanel<*>>()

    init {
        gitActionsPanel.layout = BoxLayout(gitActionsPanel, BoxLayout.X_AXIS)
        addGitButtons(gitActionsPanel)
        withMinimumHeight(JBUI.scale(30))

        changeActionsPanel.layout = BoxLayout(changeActionsPanel, BoxLayout.X_AXIS)
        addChangeButtons(changeActionsPanel)
        addRebaseProcessButtons(rebaseProcessPanel)

        if (project.service<ModelService>().rebaseInProcess) {
            changeActionsPanel.isVisible = false
            rebaseProcessPanel.isVisible = true
        }
    }

    override fun paintComponent(g: Graphics?) {
        this.layout = BorderLayout()
        super.paintComponent(g)
        this.add(gitActionsPanel, BorderLayout.WEST)
        if (changeActionsPanel.isVisible) {
            this.add(changeActionsPanel, BorderLayout.EAST)
        } else {
            this.add(rebaseProcessPanel, BorderLayout.EAST)
        }
    }

    /**
     * Add git action buttons to the header panel.
     */
    private fun addGitButtons(buttonPanel: JBPanel<JBPanel<*>>) {
        val actionsGroup =
            actionManager.getAction(
                "ActionsGroup",
            ) as RebaseActionsGroup
        val toolbar = actionManager.createActionToolbar(ActionPlaces.EDITOR_TAB, actionsGroup, true)
        val toolbarComponent: JComponent = toolbar.component
        toolbar.targetComponent = buttonPanel
        toolbarComponent.border = null
        buttonPanel.add(toolbarComponent)
        buttonPanel.border = null
    }

    /**
     * Add change action buttons to the header panel.
     */
    fun addChangeButtons(buttonPanel: JBPanel<JBPanel<*>>) {
        val group =
            actionManager.getAction(
                "ActionButtonsGroup",
            ) as RebaseActionsGroup
        val toolbar = actionManager.createActionToolbar(ActionPlaces.EDITOR_TAB, group, true)
        val toolbarComponent: JComponent = toolbar.component
        toolbar.targetComponent = buttonPanel
        toolbarComponent.border = null
        buttonPanel.add(toolbarComponent)
        buttonPanel.border = null
    }

    /**
     * Adds the continue and abort rebase buttons to the header panel.
     */
    fun addRebaseProcessButtons(buttonPanel: JBPanel<JBPanel<*>>) {
        buttonPanel.layout = BoxLayout(buttonPanel, BoxLayout.X_AXIS)
        val group =
            actionManager.getAction(
                "RebaseProcessActionsGroup",
            ) as RebaseActionsGroup
        val toolbar = actionManager.createActionToolbar(ActionPlaces.EDITOR_TAB, group, true)
        val toolbarComponent: JComponent = toolbar.component
        toolbarComponent.border = null
        toolbar.targetComponent = buttonPanel
        buttonPanel.add(toolbarComponent)
        buttonPanel.border = null
    }
}
