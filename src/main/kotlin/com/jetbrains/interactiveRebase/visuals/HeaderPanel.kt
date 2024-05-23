package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.jetbrains.interactiveRebase.actions.gitPanel.RebaseActionsGroup
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import java.awt.BorderLayout
import java.awt.Graphics
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.UIManager

class HeaderPanel(private val mainPanel: JComponent, private val project: Project, private val invoker: RebaseInvoker) :
    JBPanel<JBPanel<*>>() {
    val gitActionsPanel = JBPanel<JBPanel<*>>()
    val changeActionsPanel = JBPanel<JBPanel<*>>()

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
            ActionManager.getInstance().getAction(
                "com.jetbrains.interactiveRebase.actions.GitPanel.RebaseActionsGroup",
            ) as RebaseActionsGroup
        val toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.EDITOR_TAB, actionsGroup, true)
        val toolbarComponent: JComponent = toolbar.component
        toolbar.targetComponent = buttonPanel
        buttonPanel.add(toolbarComponent)
    }

    /**
     * Add change action buttons to the header panel.
     * At the moment, the buttons are hardcoded, but we will replace them with icons and listeners later.
     */
    private fun addChangeButtons(buttonPanel: JBPanel<JBPanel<*>>) {
        val rebaseButton = JButton("Rebase")
        rebaseButton.background = JBColor.namedColor("Button.default.startBackground", JBUI.CurrentTheme.Button.defaultButtonColorStart())
        rebaseButton.isOpaque = true
        rebaseButton.border = JBUI.Borders.empty(5, 15)
        rebaseButton.addActionListener { invoker.executeCommands() }
        val resetButton = JButton("Reset")
        resetButton.foreground = UIManager.getColor("Button.foreground")
        buttonPanel.add(resetButton)
        buttonPanel.add(rebaseButton)
    }
}
