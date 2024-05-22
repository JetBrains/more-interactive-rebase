package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.jetbrains.interactiveRebase.actions.GitPanel.RebaseActionsGroup
import com.jetbrains.interactiveRebase.actions.GitPanel.SquashAction
import com.jetbrains.interactiveRebase.listeners.DropCommitListener
import com.jetbrains.interactiveRebase.listeners.RewordButtonListener
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
    val finalizeActionsPanel = JBPanel<JBPanel<*>>()

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

//        val actionsGroup = RebaseActionsGroup()
        val actionsGroup = ActionManager.getInstance().getAction("com.jetbrains.interactiveRebase.actions.GitPanel.RebaseActionsGroup") as RebaseActionsGroup
        val toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.EDITOR_TAB, actionsGroup, true)
        val toolbarComponent : JComponent = toolbar.component
        toolbar.targetComponent = buttonPanel
        buttonPanel.add(toolbarComponent)



//        val customRebaseGroup = CustomRebaseGroup()
//        val customToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.EDITOR_TAB, customRebaseGroup, true)
//        buttonPanel.add(customToolbar.component)
//        customToolbar.targetComponent = buttonPanel


//        val squashButton = JButton("Squash")
//        val mySquashAction = SquashAction()
//        val squashButton = ActionButton(mySquashAction, )
        val fixupButton = JButton("Stop to edit")
        val rewordButton = JButton("Reword")
        rewordButton.addActionListener(RewordButtonListener(project))
        val dropButton = JButton("Drop")
        dropButton.addMouseListener(DropCommitListener(dropButton, project))
//        buttonPanel.setOKButtonText("")

//        buttonPanel.add(squashButton)
//        buttonPanel.add(fixupButton)
//        buttonPanel.add(rewordButton)
//        buttonPanel.add(dropButton)
    }

    /**
     * Add change action buttons to the header panel.
     * At the moment, the buttons are hardcoded, but we will replace them with icons and listeners later.
     */
    private fun addChangeButtons(buttonPanel: JBPanel<JBPanel<*>>) {
//        val changesGroup = IRChangeActionsGroup()
//        val toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.EDITOR_TAB, changesGroup, true)
//        val toolbarComponent : JComponent = toolbar.component
//        buttonPanel.add(toolbarComponent)
//        toolbar.targetComponent = buttonPanel

        val rebaseButton = JButton("Rebase")
//        rebaseButton.putClientProperty("JButton.buttonType", "default")
        rebaseButton.background = JBColor.namedColor("Button.default.startBackground", JBUI.CurrentTheme.Button.defaultButtonColorStart())


//        rebaseButton.isBorderPainted = false
//        rebaseButton.foreground = JBColor.namedColor("Button.default.foreground", JBUI.CurrentTheme.Button.defaultButtonColorEnd())
//        rebaseButton.background = Palette.BLUE
        rebaseButton.isOpaque = true
//        rebaseButton.isFocusPainted = true
//        rebaseButton.colorModel = JBColor.namedColor("Button.default.foreground", JBUI.CurrentTheme.Button.defaultButtonColorStart())
//        rebaseButton.border = JBUI.Borders.empty(5, 15)
        val resetButton = JButton("Reset")
        resetButton.foreground = UIManager.getColor("Button.foreground")
//        resetButton.putClientProperty("JButton.buttonType", "square")

        buttonPanel.add(resetButton)
        buttonPanel.add(rebaseButton)
    }
}
