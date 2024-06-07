package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.labels.BoldLabel
import com.intellij.ui.util.minimumWidth
import com.intellij.ui.util.preferredHeight
import com.intellij.ui.util.preferredWidth
import com.intellij.util.ui.UIUtil
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.CollapseCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.RewordCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import com.jetbrains.interactiveRebase.listeners.CircleDragAndDropListener
import com.jetbrains.interactiveRebase.listeners.CircleHoverListener
import com.jetbrains.interactiveRebase.listeners.LabelListener
import com.jetbrains.interactiveRebase.listeners.TextFieldListener
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import com.jetbrains.interactiveRebase.services.strategies.SquashTextStrategy
import java.awt.Dimension
import java.awt.Graphics
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JTextField
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

        val visualChanges = branch.currentCommits[i].getChangesAfterPick()
        if (visualChanges.any { it is CollapseCommand })
            commitLabel.text = ""
        else{
        visualChanges.forEach {
            if (it is RewordCommand) {
                commitLabel.text = TextStyle.addStyling(it.newMessage, TextStyle.ITALIC)
                commitLabel.foreground = JBColor.BLUE
            }
            if (it is DropCommand) {
                commitLabel.text = TextStyle.addStyling(commitLabel.text, TextStyle.CROSSED)
                // TODO: when drag-and-drop is implemented, this will probably break because
                // TODO: the alignment setting logic was changed
                commitLabel.horizontalAlignment = alignment
                commitLabel.alignmentX = RIGHT_ALIGNMENT
            }

            if (it is SquashCommand) {
                if (it.parentCommit == branch.currentCommits[i]) {
                    commitLabel.text = it.newMessage
                }
            }
        }

        if (branch.currentCommits[i].isSelected) {
            commitLabel.text = TextStyle.addStyling(commitLabel.text, TextStyle.BOLD)
        }}
        commitLabel.labelFor = circle
        commitLabel.horizontalAlignment = alignment
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
        labelPanelWrapper.layout = GridBagLayout()

        val circles = branchPanel.circles
        messages.clear()
        commitLabels.clear()
        for ((i, circle) in circles.withIndex()) {
            val commitLabel = generateCommitLabel(i, circle)
            val wrappedLabel = wrapLabelWithTextField(commitLabel, branch.currentCommits[i])
            wrappedLabel.preferredSize =
                Dimension(
                    wrappedLabel.preferredWidth,
                    circle.preferredHeight,
                )
            wrappedLabel.minimumSize =
                Dimension(
                    wrappedLabel.minimumWidth,
                    circle.preferredHeight,
                )
            val gbc = gridCellForCircle(i, circles)
            labelPanelWrapper.add(wrappedLabel, gbc)
            commitLabels.add(commitLabel)

            val dragAndDropListener =
                CircleDragAndDropListener(
                    project,
                    circle,
                    circles,
                    this,
                )
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
     * Sets the grid constraints
     * for a given circle index
     * and a list of circle panels.
     */
    internal fun gridCellForCircle(
        i: Int,
        circles: MutableList<CirclePanel>,
    ): GridBagConstraints {
        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = i
        gbc.weightx = 0.0
        gbc.weighty = if (i == circles.size - 1) 1.0 else 0.0
        gbc.anchor = GridBagConstraints.NORTH
        gbc.fill = GridBagConstraints.HORIZONTAL
        return gbc
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

        val gbc = gridCellForTextLabel()

        val labelWrapper = JBPanel<JBPanel<*>>()
        labelWrapper.layout = GridBagLayout()
        labelWrapper.isOpaque = false
        labelWrapper.isVisible = true

        labelWrapper.add(commitLabel, gbc)

        val textWrapper = JBPanel<JBPanel<*>>()
        textWrapper.layout = GridBagLayout()
        textWrapper.isOpaque = false
        textWrapper.isVisible = false

        val textField = createTextBox(commitLabel, commitInfo)
        textWrapper.add(textField, gbc)

        if (commitInfo.isTextFieldEnabled) {
            enableTextField(textField, textWrapper, labelWrapper, commitInfo)
        }
        textLabelWrapper.add(labelWrapper)
        textLabelWrapper.add(textWrapper)

        val labelListener = LabelListener(commitInfo)
        commitLabel.addMouseListener(labelListener)

        if (commitLabel is Disposable) {
            Disposer.register(commitLabel, labelListener)
        }

        messages.add(textLabelWrapper)

        return textLabelWrapper
    }

    private fun gridCellForTextLabel(): GridBagConstraints {
        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.anchor =
            if (alignment == SwingConstants.LEFT) {
                GridBagConstraints.LINE_START
            } else {
                GridBagConstraints.LINE_END
            }
        gbc.fill = GridBagConstraints.NONE
        return gbc
    }

    /**
     * Sets the text field to be visible,
     * called after a double click or
     * button click for rewording
     */
    private fun enableTextField(
        textField: RoundedTextField,
        textWrapper: JBPanel<JBPanel<*>>,
        labelWrapper: JBPanel<JBPanel<*>>,
        commitInfo: CommitInfo,
    ) {
        val listener = TextFieldListener(commitInfo, textField, project.service<RebaseInvoker>())
        textField.addKeyListener(listener)
        textField.requestFocusInWindow()

        setTextFieldListenerStrategy(listener, commitInfo, textField)

        textField.background = textField.background.darker()
        textWrapper.isVisible = true
        labelWrapper.isVisible = false
        SwingUtilities.invokeLater {
            textField.requestFocusInWindow()
        }
        listenForClickOutside(textField)
    }

    private fun setTextFieldListenerStrategy(
        listener: TextFieldListener,
        commitInfo: CommitInfo,
        textField: JTextField,
    ) {
        commitInfo.getChangesAfterPick().forEach {
                command ->
            if (command is SquashCommand) {
                listener.strategy = SquashTextStrategy(command, textField)
            }
        }
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
        gbc.weighty = 1.0
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
        gbc.anchor = GridBagConstraints.SOUTH
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
        revalidate()
    }

    fun getTextField(i: Int): RoundedTextField {
        val message = messages[i]
        val textWrapper = message.components[1] as JBPanel<*>
        val textField = textWrapper.components[0] as RoundedTextField
        return textField
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
