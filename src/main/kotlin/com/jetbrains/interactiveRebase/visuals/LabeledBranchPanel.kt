package com.jetbrains.interactiveRebase.visuals

import CirclePanel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.labels.BoldLabel
import com.intellij.ui.util.preferredWidth
import java.awt.*
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
    private val branchName: String,
    private val commitMessages: List<String>,
    private val alignment: Int = SwingConstants.LEFT
) :
    JBPanel<JBPanel<*>>() {
    private val branchPanel = BranchPanel(commitMessages)
    private val commitLabels: MutableList<JBLabel> = mutableListOf()
    private val branchNameLabel = BoldLabel(branchName)

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
    private fun generateCommitLabel(i: Int, circle: CirclePanel): JBLabel {
        val commitLabel = JBLabel(commitMessages[i])
        commitLabel.labelFor = circle
        commitLabel.preferredSize = Dimension(commitLabel.preferredWidth, branchPanel.DIAMETER)
        commitLabel.alignmentX =
            (if (alignment == SwingConstants.LEFT)
                LEFT_ALIGNMENT
            else
                RIGHT_ALIGNMENT)
        commitLabel.verticalTextPosition = SwingConstants.CENTER
        return commitLabel
    }

    override fun addNotify() {
        super.addNotify()

        layout = GridBagLayout()
        val gbc = GridBagConstraints()
        gbc.gridx = if (alignment == SwingConstants.LEFT) 0 else 1
        gbc.gridy = 0
        gbc.weightx = 1.0
        gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)
        add(branchNameLabel, gbc)

        gbc.gridx = if (alignment == SwingConstants.LEFT) 0 else 1
        gbc.gridy = 1
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        gbc.insets = Insets(5, 5, 5, 5)
        add(branchPanel, gbc)

        val labelPanelWrapper = JBPanel<JBPanel<*>>()
        labelPanelWrapper.layout = BoxLayout(labelPanelWrapper, BoxLayout.Y_AXIS)
        for (i in commitLabels.indices) {
            labelPanelWrapper.add(commitLabels[i])
            if (i < commitLabels.size - 1) {
                labelPanelWrapper.add(Box.createVerticalGlue())
            }
        }

        gbc.gridx = if (alignment == SwingConstants.LEFT) 1 else 0
        gbc.gridy = 1
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        gbc.insets = Insets(5, 5, 5, 5)
        add(labelPanelWrapper, gbc)
    }
}