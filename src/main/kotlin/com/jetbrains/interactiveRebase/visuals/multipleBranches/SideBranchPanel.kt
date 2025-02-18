package com.jetbrains.interactiveRebase.visuals.multipleBranches

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.util.preferredHeight
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
    var isHovered: Boolean = false
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
        backgroundColor = Palette.TRANSPARENT
        isOpaque = false
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
    }

    /**
     * Adds the branch name to the panel
     */
    internal fun addSideBranchLabel() {
        label = JBLabel(branchName)
        label.preferredSize = Dimension(1, label.preferredHeight)

        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.LINE_START
        gbc.insets = JBUI.insets(5, 5, 5, 5)

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
        button.maximumSize = Dimension((1.4 * button.icon.iconWidth).toInt(), (1.4 * button.icon.iconHeight).toInt())
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
        gbc.weightx = 0.0
        gbc.anchor = GridBagConstraints.LINE_END
        gbc.insets = JBUI.insetsRight(10)
        return gbc
    }

    /**
     * Changes the color of the panel when the mouse hovers over it.
     */
    internal fun onHover() {
        backgroundColor = Palette.JETBRAINS_HOVER
        isHovered = true
        this.repaint()
        this.revalidate()
    }

    /**
     * Resets the panel to its original state.
     */
    internal fun resetSideBranchPanelVisually() {
        this.isOpaque = false
        this.isHovered = false
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
            return triggerWarningForAddingBranch()
        }
        addSelectedBranchToView()
        return true
    }

    /**
     * Adds the selected branch to the graph visualization next to the checked out branch
     */
    private fun addSelectedBranchToView() {
        modelService.addSecondBranchToGraphInfo(branchName, 0)
        selectBranchVisually()
    }

    /**
     * Selects a branch panel by only making the visual changes,
     * does not actually select the branch and add it to the view
     */
    fun selectBranchVisually() {
        this.isOpaque = false
        backgroundColor = Palette.JETBRAINS_SELECTED
        this.isSelected = true
        this.button.isVisible = true
        this.repaint()
        this.revalidate()
    }

    /**
     * Triggers an overwriting changes warning, returns true if the answer to the warning is yes, false otherwise.
     * Goes through with the removal if the change is made
     */
    internal fun deselectBranch(): Boolean {
        if (modelService.graphInfo.addedBranch?.baseCommit == null &&
            modelService.graphInfo.mainBranch.currentCommits.isNotEmpty()
        ) {
            return false
        }
        if (project.service<RebaseInvoker>().commands.isNotEmpty()) {
            val answer: Boolean =
                dialogService.warningYesNoDialog(
                    "Overwriting Changes",
                    "Removing this branch from the view will reset the actions you have made. Do you want to continue?",
                )
            // if the answer is no, do not remove the branch
            if (!answer) return false
        }
        modelService.removeSecondBranchFromGraphInfo(0)
        return true
    }

    /**
     * Shows dialogue stating that the staged changes will be overwritten
     * If branch is added and yes is chosen, returns true, false otherwise
     */
    private fun triggerWarningForAddingBranch(): Boolean {
        val answer: Boolean =
            dialogService.warningYesNoDialog(
                "Overwriting Changes",
                "Adding another branch to the view will reset the actions you have made. Do you want to continue?",
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
        if (modelService.graphInfo.addedBranch?.baseCommit == null &&
            modelService.graphInfo.mainBranch.currentCommits.isNotEmpty()
        ) {
            return
        }
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
