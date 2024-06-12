package com.jetbrains.interactiveRebase.actions.ButtonActions

import com.intellij.ide.HelpTooltip
import com.intellij.ide.IdeBundle
import com.intellij.ide.actions.GotoClassPresentationUpdater
import com.intellij.ide.ui.laf.darcula.ui.DarculaButtonPainter
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.actionSystem.ex.TooltipDescriptionProvider
import com.intellij.openapi.actionSystem.ex.TooltipLinkProvider
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.Strings
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import java.awt.Component
import java.awt.Dimension
import java.awt.Insets
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComponent

internal class RebaseAction() :
        ButtonAction("Rebase", "Start the rebase process") {

     override fun actionPerformed(e: AnActionEvent) {
         val invoker = e.project?.service<RebaseInvoker>()
         invoker?.createModel()
         invoker?.executeCommands()

    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }

    override fun update(e: AnActionEvent) {
        e.project?.service<ActionService>()?.checkRebase(e)

    }


}
internal abstract class ButtonAction(title: String, description: String,
                                     additionalShortcuts: List<Shortcut> = listOf()
) : IRAction(title, description,null,  additionalShortcuts), CustomComponentAction, DumbAware {
    protected val button = object : JButton(title) {
        init {
            val buttonHeight = JBUI.scale(28)
            preferredSize = Dimension(preferredSize.width, buttonHeight)
            border = object : DarculaButtonPainter() {
                override fun getBorderInsets(c: Component?): Insets {
                    return JBInsets.emptyInsets()
                }
            }
            isFocusable = false
            addActionListener {
                val dataContext = ActionToolbar.getDataContextFor(this)
                actionPerformed(
                        AnActionEvent.createFromAnAction(this@ButtonAction,
                                null, ActionPlaces.EDITOR_TAB, dataContext)
                )
            }
            val os = System.getProperty("os.name").toLowerCase()
            val shortcutText = if (os.contains("mac")) "Option+Enter" else "Alt+Enter"

            if (Registry.`is`("ide.helptooltip.enabled")) {
                HelpTooltip.dispose(this)
                HelpTooltip()
                        .setTitle(this.text)
                        .setShortcut("Alt+Enter")
                        .setDescription(description)
                        .installOn(this)
            }

        }

    }

    private val buttonPanel = BorderLayoutPanel().addToCenter(button).apply {
        border = JBUI.Borders.emptyLeft(6)
    }

    override fun updateCustomComponent(component: JComponent, presentation: Presentation) {
        button.isEnabled = presentation.isEnabled
    }
    override fun createCustomComponent(presentation: Presentation, place: String) : JComponent {
        return buttonPanel
    }


}
internal abstract class IRAction(title: String, description: String, icon: Icon?,
        additionalShortcuts: List<Shortcut> = listOf()
) :  DumbAwareAction(title, description, icon) {


}