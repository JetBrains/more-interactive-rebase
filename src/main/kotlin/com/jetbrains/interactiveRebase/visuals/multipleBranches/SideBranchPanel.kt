package com.jetbrains.interactiveRebase.visuals.multipleBranches

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.jetbrains.interactiveRebase.services.DialogService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import com.jetbrains.interactiveRebase.visuals.Palette
import com.jetbrains.interactiveRebase.visuals.RoundedButton
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JLabel

class SideBranchPanel(val branchName: String, val project: Project) : RoundedPanel(), Disposable {
    var isSelected: Boolean = false
    lateinit var label: JLabel
    lateinit var button: RoundedButton
    var dialogService: DialogService = project.service<DialogService>()
    var modelService: ModelService = project.service<ModelService>()

    /**
     * Constructor used in tests for dependency injection
     */
    constructor(
        branchName: String,
        project: Project,
        dialogService: DialogService,
        modelService: ModelService,
    ) : this(branchName, project) {
        this.dialogService = dialogService
        this.modelService = modelService
    }

    init {
        backgroundColor = background
        cornerRadius = 15
        createSideBranchPanel()
    }

    /**
     * Creates the panel which will hold the branch name and
     * a button to remove it.
     */
    internal fun createSideBranchPanel() {
        layout = GridBagLayout()
        addSideBranchLabel()
        addRemoveBranchButton()

        val name = modelService.graphInfo.addedBranch?.name

        if (name == branchName) selectBranch()
    }

    /**
     * Adds the branch name to the panel
     */
    internal fun addSideBranchLabel() {
        label = JBLabel(branchName)

        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.9
        gbc.anchor = GridBagConstraints.LINE_START
        gbc.insets = JBUI.insets(5, 20, 5, 5)

        add(label, gbc)
    }

    /**
     * Adds the button to remove the branch from the panel
     */
    internal fun addRemoveBranchButton() {
        button = RoundedButton()

        button.icon = AllIcons.General.Remove
        button.arcHeight = 5
        button.arcWidth = 5
        button.border = null
        button.preferredSize = Dimension((1.4 * button.icon.iconWidth).toInt(), (1.4 * button.icon.iconHeight).toInt())
        button.isOpaque = false
        button.setContentAreaFilled(false)
        button.isVisible = false

        val gbc = getAlignmentForButton()
        add(button, gbc)
    }

    /**
     * Aligns the button to the right of the panel,
     * with a small margin from the right edge.
     */
    internal fun getAlignmentForButton(): GridBagConstraints {
        val gbc = GridBagConstraints()
        gbc.gridx = 1
        gbc.weightx = 0.1
        gbc.anchor = GridBagConstraints.LINE_END
        gbc.insets = JBUI.insetsRight(10)
        return gbc
    }

    /**
     * Changes the color of the panel when the mouse hovers over it.
     */
    internal fun onHover() {
        backgroundColor = Palette.JETBRAINS_HOVER
        this.repaint()
        this.revalidate()
    }

    /**
     * Resets the panel to its original state.
     */
    internal fun resetSideBranchPanelVisually() {
        this.isOpaque = false
        this.isSelected = false
        this.button.isVisible = false
        this.label.foreground = JBColor.BLACK
        backgroundColor = Palette.TRANSPARENT
        this.repaint()
        this.revalidate()
    }

    /**
     * Changes the color of the panel when it is selected.
     * Triggers warning if there are any changes in the invoker, if not, adds the selected branch to the view
     * Returns true if branch is successfully added, false if adding was cancelled after warning
     */
    internal fun selectBranch(): Boolean {
        // if there are staged changes, trigger warning that they might be overwritten
        if (project.service<RebaseInvoker>().commands.isNotEmpty()) {
            return triggerOverwriteWarning()
        }
        addSelectedBranchToView()
        return true
    }

    /**
     * Adds the selected branch to the graph visualization next to the checked out branch
     */
    private fun addSelectedBranchToView() {
        modelService.addSecondBranchToGraphInfo(branchName)
        this.isOpaque = true
        backgroundColor = Palette.JETBRAINS_SELECTED
        this.isSelected = true
        this.button.isVisible = true
        this.repaint()
        this.revalidate()
    }

    /**
     * Shows dialogue stating that the staged changes will be overwritten
     * If branch is added and yes is chosen, returns true, false otherwise
     */
    private fun triggerOverwriteWarning(): Boolean {
        val answer: Boolean =
            dialogService.warningYesNoDialog(
                "Overwriting Staged Changes",
                "Adding another branch to the view will reset the staged interactive rebase actions. Do you want to continue?",
            )
        if (answer) {
            addSelectedBranchToView()
            return true
        }
        return false
    }

    /**
     * Changes the color of the branch name (label) to gray.
     */
    internal fun grayOutText() {
        this.label.foreground = Palette.GRAY_BUTTON
    }

    /**
     * Changes the color of the button when the mouse hovers over it.
     */
    internal fun buttonOnHover() {
        button.backgroundColor = backgroundColor.darker()

        button.repaint()
        button.revalidate()
    }

    /**
     * Resets the button to its original state.
     */
    internal fun buttonOnHoverExit() {
        button.backgroundColor = Palette.TRANSPARENT

        button.repaint()
        button.revalidate()
    }

    override fun dispose() {
    }
}
