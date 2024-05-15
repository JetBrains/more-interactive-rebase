package com.jetbrains.interactiveRebase.visuals

import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.labels.BoldLabel
import com.intellij.ui.util.preferredWidth
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.SwingConstants

/**
 * Panel encapsulating a branch and corresponding labels
 * - Branch name on top
 * - Each commit name
 * Changing the alignment attribute, changes the layout of the branch
 * - LEFT means commits appear to the left and names to the right
 * - RIGHT means commits appear to the right and names to the left
 */
class LabeledBranchPanel(
    private val branch: BranchInfo,
    private val color: JBColor,
    private val alignment: Int = SwingConstants.LEFT,
) :
    JBPanel<JBPanel<*>>() {
    val branchPanel = BranchPanel(branch, color)
    private val commitLabels: MutableList<JBLabel> = mutableListOf()
    private val branchNameLabel = BoldLabel(branch.name)

    init {
        branchNameLabel.horizontalTextPosition = SwingConstants.CENTER
        val circles = branchPanel.getCirclePanels()
        for ((i, circle) in circles.withIndex()) {
            val commitLabel = generateCommitLabel(i, circle)
            commitLabels.add(commitLabel)
        }
    }

    /**
     * Sets up the appearance of a commit label
     * and links it to the corresponding commit (circle panel)
     */
    fun generateCommitLabel(
        i: Int,
        circle: CirclePanel,
    ): JBLabel {
        val commitLabel = JBLabel(branch.commits[i].getSubject())
        commitLabel.labelFor = circle
        commitLabel.preferredSize = Dimension(commitLabel.preferredWidth, branchPanel.diameter)
        commitLabel.alignmentX =
            (
                if (alignment == SwingConstants.LEFT) {
                    LEFT_ALIGNMENT
                } else {
                    RIGHT_ALIGNMENT
                }
            )
        commitLabel.verticalTextPosition = SwingConstants.CENTER
        return commitLabel
    }

    /**
     * Draws the branch with the added labels.
     */

    override fun addNotify() {
        super.addNotify()

        layout = GridBagLayout()
        val gbc = GridBagConstraints()

        setBranchNamePosition(gbc)
        add(branchNameLabel, gbc)

        setBranchPosition(gbc)
        add(branchPanel, gbc)

        val labelPanelWrapper = JBPanel<JBPanel<*>>()
        wrapCommitLabels(labelPanelWrapper)

        setCommitNamesPosition(gbc)
        add(labelPanelWrapper, gbc)
    }

    /**
     * Puts all commit labels in a panel that
     * serves as a wrapper.
     */
    fun wrapCommitLabels(labelPanelWrapper: JBPanel<JBPanel<*>>) {
        labelPanelWrapper.layout = BoxLayout(labelPanelWrapper, BoxLayout.Y_AXIS)
        for (i in commitLabels.indices) {
            labelPanelWrapper.add(commitLabels[i])
            if (i < commitLabels.size - 1) {
                labelPanelWrapper.add(Box.createVerticalGlue())
            }
        }
    }

    /**
     * Sets the position of the commit names
     * on the grid.
     */
    fun setCommitNamesPosition(gbc: GridBagConstraints) {
        gbc.gridx = if (alignment == SwingConstants.LEFT) 1 else 0
        gbc.gridy = 1
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        gbc.insets = Insets(5, 5, 5, 5)
    }

    /**
     * Sets the position of the branch
     * on the grid.
     */
    fun setBranchPosition(gbc: GridBagConstraints) {
        gbc.gridx = if (alignment == SwingConstants.LEFT) 0 else 1
        gbc.gridy = 1
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        gbc.insets = Insets(5, 5, 5, 5)
    }

    /**
     * Sets the position of the branch name label
     * on the grid.
     */
    fun setBranchNamePosition(gbc: GridBagConstraints) {
        gbc.gridx = if (alignment == SwingConstants.LEFT) 0 else 1
        gbc.gridy = 0
        gbc.weightx = 1.0
        gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)
    }
}
