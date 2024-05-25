package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.labels.BoldLabel
import com.intellij.ui.util.preferredWidth
import com.intellij.util.ui.UIUtil
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.RewordCommand
import com.jetbrains.interactiveRebase.listeners.CircleDragAndDropListener
import com.jetbrains.interactiveRebase.listeners.CircleHoverListener
import com.jetbrains.interactiveRebase.listeners.LabelListener
import com.jetbrains.interactiveRebase.listeners.TextFieldListener
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Graphics
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.Insets
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.OverlayLayout
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

/**
 * Panel encapsulating a branch and corresponding labels
 * - Branch name on top
 * - Each commit name
 * Changing the alignment attribute, changes the layout of the branch
 * - LEFT means commits appear to the left and names to the right
 * - RIGHT means commits appear to the right and names to the left
 */
class LabeledBranchPanel(
    val project: Project,
    val invoker: RebaseInvoker,
    val branch: BranchInfo,
    private val color: JBColor,
    private val alignment: Int = SwingConstants.LEFT,
) :
    JBPanel<JBPanel<*>>(), Disposable {
    val branchPanel = BranchPanel(branch, color)
    val commitLabels: MutableList<JBLabel> = mutableListOf()
    val messages: MutableList<JBPanel<JBPanel<*>>> = mutableListOf()
    private val branchNameLabel = BoldLabel(branch.name)
    internal val labelPanelWrapper = JBPanel<JBPanel<*>>()

    init {
        branchNameLabel.horizontalAlignment = SwingConstants.CENTER

        layout = GridBagLayout()
        val gbc = GridBagConstraints()

        addComponents()

        setBranchNamePosition(gbc)
        add(branchNameLabel, gbc)

        setBranchPosition(gbc)
        add(branchPanel, gbc)

        setCommitNamesPosition(gbc)
        add(labelPanelWrapper, gbc)
    }

    /**
     * Sets up the appearance of a commit label
     * and links it to the corresponding commit (circle panel)
     * If a commit is selected, the text is bold, italic if it is reworded, and crossed if it is dropped
     */
    fun generateCommitLabel(
        i: Int,
        circle: CirclePanel,
    ): JBLabel {
        val truncatedMessage = truncateMessage(i)

        val commitLabel =
            object : JBLabel(truncatedMessage), Disposable {
                override fun dispose() {
                }
            }

        branch.currentCommits[i].changes.forEach {
            if (it is RewordCommand) {
                commitLabel.text = TextStyle.addStyling(it.newMessage, TextStyle.ITALIC)
            }
            if (it is DropCommand) {
                commitLabel.text = TextStyle.addStyling(commitLabel.text, TextStyle.CROSSED)
                // TODO: when drag-and-drop is implemented, this will probably break because
                // TODO: the alignment setting logic was changed
                commitLabel.horizontalAlignment = alignment
                commitLabel.alignmentX = RIGHT_ALIGNMENT
            }
        }

        if (branch.currentCommits[i].isSelected) {
            commitLabel.text = TextStyle.addStyling(commitLabel.text, TextStyle.BOLD)
        }

        commitLabel.fontColor = UIUtil.FontColor.NORMAL
        commitLabel.labelFor = circle
        commitLabel.horizontalAlignment = alignment
        commitLabel.preferredSize = Dimension(commitLabel.preferredWidth, branchPanel.diameter)
        commitLabel.verticalTextPosition = SwingConstants.CENTER

        return commitLabel
    }

    private fun truncateMessage(i: Int): String {
        val maxCharacters = 500
        val commitMessage = branch.currentCommits[i].commit.subject
        val truncatedMessage =
            if (commitMessage.length > maxCharacters) {
                "${commitMessage.substring(0, maxCharacters)}..."
            } else {
                commitMessage
            }
        return truncatedMessage
    }

    /**
     * Generates the panel in which commit labels are wrapped with invisible text fields
     */
    fun addComponents() {
        labelPanelWrapper.layout = GridLayout(0, 1)
        val circles = branchPanel.circles
        messages.clear()
        for ((i, circle) in circles.withIndex()) {
            val commitLabel = generateCommitLabel(i, circle)
            val wrappedLabel = wrapLabelWithTextField(commitLabel, branch.currentCommits[i])
            labelPanelWrapper.add(wrappedLabel)
            commitLabels.add(commitLabel)

            val dragAndDropListener = CircleDragAndDropListener(project, circle, circles, this)
            circle.addMouseListener(dragAndDropListener)
            circle.addMouseMotionListener(dragAndDropListener)
            Disposer.register(this, dragAndDropListener)

            val circleHoverListener = CircleHoverListener(circle)
            circle.addMouseListener(circleHoverListener)
            circle.addMouseMotionListener(circleHoverListener)
            Disposer.register(this, circleHoverListener)
        }
    }

    /**
     * Wraps the given label together with a text field that is only made visible if it is triggered.
     * There is one panel (textLabelWrapper) that wraps a textFieldWrapper with a labelWrapper, each being panels
     * that contain a text field and a label respectively in order to properly align them.
     */
    fun wrapLabelWithTextField(
        commitLabel: JBLabel,
        commitInfo: CommitInfo,
    ): JComponent {
        val textLabelWrapper = JBPanel<JBPanel<*>>()
        textLabelWrapper.layout = OverlayLayout(textLabelWrapper)
        textLabelWrapper.isOpaque = false

        val textWrapper = JBPanel<JBPanel<*>>()
        textWrapper.layout = FlowLayout(alignment)
        textWrapper.isOpaque = false

        val textField = createTextBox(commitLabel, commitInfo)
        textWrapper.add(textField)

        val labelWrapper = JBPanel<JBPanel<*>>()
        labelWrapper.layout = FlowLayout(alignment)
        labelWrapper.isOpaque = false

        labelWrapper.add(commitLabel)

        textWrapper.isVisible = false
        labelWrapper.isVisible = true

        if (commitInfo.isDoubleClicked) {
            enableTextField(textField, textWrapper, labelWrapper)
        }
        textLabelWrapper.add(labelWrapper)
        textLabelWrapper.add(textWrapper)

        val labelListener = LabelListener(commitInfo)
        commitLabel.addMouseListener(labelListener)
        if (commitLabel is Disposable) {
            Disposer.register(commitLabel, labelListener)
        }
        textField.addKeyListener(TextFieldListener(commitInfo, textField, invoker))

        messages.add(textLabelWrapper)
        return textLabelWrapper
    }

    /**
     * Sets the text field to be visible, called after a double-click or button click for rewording
     */
    private fun enableTextField(
        textField: RoundedTextField,
        textWrapper: JBPanel<JBPanel<*>>,
        labelWrapper: JBPanel<JBPanel<*>>,
    ) {
        textField.background = textField.background.darker()
        textWrapper.isVisible = true
        labelWrapper.isVisible = false
        textField.requestFocusInWindow()
        listenForClickOutside(textField)
    }

    /**
     * Instantiates a listener that exits the reword textbox when somewhere else on the component is clicked
     */
    private fun listenForClickOutside(textField: RoundedTextField) {
        this.addMouseListener(
            object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (textField.isVisible && e.component !== textField) {
                        textField.exitTextBox()
                    }
                }
            },
        )
    }

    /**
     * Instantiates a round cornered textbox for rewording
     */
    fun createTextBox(
        commitLabel: JBLabel,
        commitInfo: CommitInfo,
    ): RoundedTextField {
        val textField = RoundedTextField(commitInfo, TextStyle.stripTextFromStyling(commitLabel.text), color)
        SwingUtilities.invokeLater {
            textField.maximumSize = commitLabel.size
        }
        textField.horizontalAlignment = alignment
        return textField
    }

    /**
     * Sets the position of the commit names
     * on the grid.
     */
    fun setCommitNamesPosition(gbc: GridBagConstraints) {
        gbc.gridx = if (alignment == SwingConstants.LEFT) 1 else 0
        gbc.gridy = 1
        gbc.weightx = 1.0
        gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.BOTH
        gbc.anchor = GridBagConstraints.CENTER
        gbc.insets = Insets(5, 5, 5, 5)
    }

    /**
     * Sets the position of the branch
     * on the grid.
     */
    fun setBranchPosition(gbc: GridBagConstraints) {
        gbc.gridx = if (alignment == SwingConstants.LEFT) 0 else 1
        gbc.gridy = 1
        gbc.weightx = 0.0
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
        gbc.weightx = 0.0
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)
    }

    /**
     * Updates branch name
     */
    fun updateBranchName() {
        branchNameLabel.text = branch.name
    }

    /**
     * Sets commits to be shown in branch
     */
    fun updateCommits() {
        commitLabels.clear()
        branchPanel.updateCommits()
        val circles = branchPanel.circles
        for ((i, circle) in circles.withIndex()) {
            val commitLabel = generateCommitLabel(i, circle)
            commitLabels.add(commitLabel)
        }

        labelPanelWrapper.removeAll()
        addComponents()
    }

    override fun paintComponent(g: Graphics?) {
        super.paintComponent(g)

        for (commitLabel in commitLabels) {
            if ((commitLabel.labelFor as CirclePanel).commit.isDragged) {
                commitLabel.foreground = JBColor.BLUE
            } else {
                commitLabel.fontColor = UIUtil.FontColor.NORMAL
            }
        }
    }

    /**
     * Dispose routine
     */
    override fun dispose() {
    }
}
