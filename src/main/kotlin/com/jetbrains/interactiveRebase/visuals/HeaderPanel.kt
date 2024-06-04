package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.actions.gitPanel.RebaseActionsGroup
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import java.awt.BorderLayout
import java.awt.Graphics
import javax.swing.BoxLayout
import javax.swing.JComponent

class HeaderPanel(private val project: Project, private val actionManager: ActionManager = ActionManager.getInstance()) :
    JBPanel<JBPanel<*>>() {
    private val gitActionsPanel = JBPanel<JBPanel<*>>()
    val changeActionsPanel = JBPanel<JBPanel<*>>()
    private val invoker = project.service<RebaseInvoker>()

    init {
        gitActionsPanel.layout = BoxLayout(gitActionsPanel, BoxLayout.X_AXIS)
        addGitButtons(gitActionsPanel)

        changeActionsPanel.layout = BoxLayout(changeActionsPanel, BoxLayout.X_AXIS)
        addChangeButtons(changeActionsPanel)
    }

    override fun paintComponent(g: Graphics?) {
        this.layout = BorderLayout()
        super.paintComponent(g)
        this.add(gitActionsPanel, BorderLayout.WEST)
        this.add(changeActionsPanel, BorderLayout.EAST)
    }

    /**
     * Add git action buttons to the header panel.
     * At the moment, the buttons are hardcoded, but we will replace them with icons and listeners later.
     */
    private fun addGitButtons(buttonPanel: JBPanel<JBPanel<*>>) {
        val actionsGroup =
            actionManager.getAction(
                "com.jetbrains.interactiveRebase.actions.gitPanel.RebaseActionsGroup",
            ) as RebaseActionsGroup
        val toolbar = actionManager.createActionToolbar(ActionPlaces.EDITOR_TAB, actionsGroup, true)
        val toolbarComponent: JComponent = toolbar.component
        toolbar.targetComponent = buttonPanel
        buttonPanel.add(toolbarComponent)
    }

    /**
     * Add change action buttons to the header panel.
     * At the moment, the buttons are hardcoded, but we will replace them with icons and listeners later.
     */
    fun addChangeButtons(buttonPanel: JBPanel<JBPanel<*>>) {
        val rebaseButton = RoundedButton("Rebase", Palette.BLUEBUTTON, Palette.WHITETEXT)

        rebaseButton.addActionListener {
            invoker.createModel()
            invoker.executeCommands()
        }
        val resetButton = RoundedButton("Reset", Palette.GRAYBUTTON, Palette.WHITETEXT)

        resetButton.addActionListener { project.service<ActionService>().resetAllChangesAction() }

        buttonPanel.add(resetButton)
        buttonPanel.add(rebaseButton)
    }
}
