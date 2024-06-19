package com.jetbrains.interactiveRebase.visuals

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.labels.BoldLabel
import com.intellij.ui.util.minimumHeight
import com.intellij.ui.util.minimumWidth
import com.intellij.ui.util.preferredHeight
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.CollapseCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.RewordCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import com.jetbrains.interactiveRebase.listeners.*
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import com.jetbrains.interactiveRebase.services.strategies.SquashTextStrategy
import com.jetbrains.interactiveRebase.visuals.multipleBranches.RoundedPanel
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.border.EmptyBorder

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
    val colorTheme: Palette.Theme,
    private val alignment: Int = SwingConstants.LEFT,
) :
    JBPanel<JBPanel<*>>(), Disposable {
    val branchPanel = BranchPanel(branch, colorTheme)
    val commitLabels: MutableList<JBLabel> = mutableListOf()
    val messages: MutableList<JBPanel<JBPanel<*>>> = mutableListOf()
    val branchNamePanel = branchNamePanel()

    internal val labelPanelWrapper = JBPanel<JBPanel<*>>()

    init {
        labelPanelWrapper.isOpaque = false
        layout = GridBagLayout()
        isOpaque = false
        val gbc = GridBagConstraints()

        addComponents()

        addBranchName(gbc)

        addBranchOfCommits(gbc)

        addCommitNames(gbc)
    }

    /**
     * Add the labels containing the commit names
     * to the panel
     * You can specify offset if you want to move them up/down
     */
    private fun LabeledBranchPanel.addCommitNames(
        gbc: GridBagConstraints,
        offset: Int = 5,
    ) {
        setCommitNamesPosition(gbc, offset)
        add(labelPanelWrapper, gbc)
    }

    /**
     * Add the connected commits to the panel
     * You can specify offset if you want to move them up/down
     */
    private fun LabeledBranchPanel.addBranchOfCommits(
        gbc: GridBagConstraints,
        offset: Int = 5,
    ) {
        setBranchPosition(gbc, offset)
        add(branchPanel, gbc)
    }

    /**
     * Add the branch name to the panel
     */
    private fun LabeledBranchPanel.addBranchName(gbc: GridBagConstraints) {
        setBranchNamePosition(gbc)
        add(branchNamePanel, gbc)
    }

    /**
     * Function that adds the branch
     * with the corresponding name labels
     * with specified offset
     * Call when you want to change alignment of
     * secondary branch
     */
    fun addBranchWithVerticalOffset(offset: Int = 5) {
        remove(labelPanelWrapper)
        remove(branchPanel)
        val gbc = GridBagConstraints()
        addCommitNames(gbc, offset)
        addBranchOfCommits(gbc, offset)
    }

    private fun branchNamePanel(): JBPanel<*> {
        val panel = instantiateBranchNamePanel()

        panel.addMouseListener(
            object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent?) {
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    if (branch.isRebased) {
                        (panel.getComponent(0) as RoundedPanel).backgroundColor = Palette.TRANSPARENT
                    } else {
                        (panel.getComponent(0) as RoundedPanel).backgroundColor = colorTheme.regularCircleColor
                    }
                    panel.repaint()
                }

                override fun mouseExited(e: MouseEvent?) {
                    cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                    if (branch.isRebased) {
                        (panel.getComponent(0) as RoundedPanel).backgroundColor = Palette.TRANSPARENT
                    } else {
                        (panel.getComponent(0) as RoundedPanel).backgroundColor = colorTheme.branchNameColor
                    }
                    panel.repaint()
                }
            },
        )

        return panel
    }

    fun instantiateBranchNamePanel(): JBPanel<*> {
        val panel = JBPanel<JBPanel<*>>()
        panel.isOpaque = false
        panel.layout = GridBagLayout()
        val label = BoldLabel(branch.name)
        label.horizontalAlignment = SwingConstants.CENTER
        val roundedPanel = RoundedPanel()
        roundedPanel.border = EmptyBorder(2, 3, 3, 3)
        roundedPanel.cornerRadius = 15
        roundedPanel.removeBackgroundGradient()
        roundedPanel.backgroundColor = colorTheme.branchNameColor
        if (branch.isRebased) {
            roundedPanel.backgroundColor = Palette.TRANSPARENT
            roundedPanel.addBackgroundGradient(colorTheme.branchNameColor, Palette.TOMATO_THEME.branchNameColor)
        }
        roundedPanel.add(label)
        panel.add(roundedPanel)
        if (branch.isPrimary) {
            addHelpMessage(panel)
        }
        return panel
    }

    /**
     * Adds an indication that the branch name label
     * of the main branch panel
     * can be dragged in the form of
     * a help message beneath the label
     */
    private fun addHelpMessage(panel: JBPanel<JBPanel<*>>) {
        val help = JBPanel<JBPanel<*>>()
        help.layout = GridBagLayout()
        help.isOpaque = false
        addRebaseIcon(help)
        addTextMessage(help)
        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.anchor = GridBagConstraints.CENTER
        panel.add(help, gbc)
    }

    /**
     * Adds a help message beneath
     * the branch name label
     * of the main branch panel
     */
    private fun addTextMessage(help: JBPanel<JBPanel<*>>) {
        val msg = TextStyle.addStyling("drag to rebase", TextStyle.ITALIC)
        val helpMsg = JBLabel(msg)
        helpMsg.minimumSize = Dimension(helpMsg.minimumWidth, 22)
        helpMsg.isOpaque = false
        helpMsg.foreground = JBColor.LIGHT_GRAY.brighter()

        val gbc = GridBagConstraints()
        gbc.gridx = 1
        help.add(helpMsg)
    }

    /**
     * Adds the rebase icon at the bottom
     * of the branch name label
     * of the main branch panel
     */
    private fun addRebaseIcon(help: JBPanel<JBPanel<*>>) {
        val iconPanel = object : JBPanel<JBPanel<*>>() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D
                val originalComposite = g2d.composite
                g2d.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f)
                AllIcons.Vcs.Branch.paintIcon(this, g2d, 2, 2)
                g2d.composite = originalComposite
            }
        }
        iconPanel.isOpaque = false
        iconPanel.preferredSize = Dimension(20, 20)
        help.add(iconPanel)
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

        val commitLabel = JBLabel(truncatedMessage)

        val visualChanges = branch.currentCommits[i].getChangesAfterPick()
        if (visualChanges.any { it is CollapseCommand }) {
            commitLabel.text = ""
        } else {
            visualChanges.forEach {
                if (it is RewordCommand) {
                    commitLabel.text = TextStyle.addStyling(it.newMessage, TextStyle.ITALIC)
                    commitLabel.foreground = JBColor.BLUE
                }
                if (it is DropCommand) {
                    commitLabel.text = TextStyle.addStyling(commitLabel.text, TextStyle.CROSSED)
                    commitLabel.foreground = JBColor.LIGHT_GRAY.brighter()
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
            }
        }
        commitLabel.autoscrolls = true
        commitLabel.labelFor = circle
        commitLabel.horizontalAlignment = alignment
        commitLabel.verticalTextPosition = SwingConstants.CENTER

        return commitLabel
    }

    /**
     * If message longer than 500 characters
     * shorten it by putting ellipsis ...
     */
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
        labelPanelWrapper.minimumSize = Dimension(JBUI.scale(300), labelPanelWrapper.minimumHeight)

        val circles = branchPanel.circles
        messages.clear()
        commitLabels.clear()
        for ((i, circle) in circles.withIndex()) {
            val commitLabel = generateCommitLabel(i, circle)
            val wrappedLabel = wrapLabelWithTextField(commitLabel, branch.currentCommits[i])
            wrappedLabel.preferredSize =
                Dimension(
                    wrappedLabel.minimumWidth,
                    circle.preferredHeight,
                )
            wrappedLabel.minimumSize =
                Dimension(
                    wrappedLabel.minimumWidth,
                    circle.preferredHeight,
                )
            val gbc = gridCellForCircle(i, circles)
            if (i == 0) {
                gbc.insets.top = branchPanel.diameter
            }
            gbc.weightx = 1.0
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

            if (
                !branch.isWritable &&
                circle !is CollapseCirclePanel &&
                !circle.commit.wasCherryPicked
            ) {
                val cherryDragAndDropListener =
                    CherryDragAndDropListener(
                        project,
                        circle,
                        this,
                    )
                circle.addMouseListener(cherryDragAndDropListener)
                circle.addMouseMotionListener(cherryDragAndDropListener)
                Disposer.register(this, cherryDragAndDropListener)
            }
        }
    }

    /**
     * Sets the grid constraints
     * for a given circle index
     * and a list of circle panels.
     */
    private fun gridCellForCircle(
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

        val labelListener = LabelListener(commitInfo, branch)
        commitLabel.addMouseListener(labelListener)

        if (commitLabel is Disposable) {
            Disposer.register(commitLabel, labelListener)
        }

        messages.add(textLabelWrapper)

        return textLabelWrapper
    }

    /**
     * Specify alignment within the grid
     * of the text label
     */
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
        commitInfo.getChangesAfterPick().forEach { command ->
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
        val textField =
            RoundedTextField(
                commitInfo,
                TextStyle.stripTextFromStyling(commitLabel.text),
                colorTheme.regularCircleColor,
            )
        textField.minimumSize = Dimension(JBUI.scale(300), textField.minimumHeight)
        textField.horizontalAlignment = alignment
        return textField
    }

    /**
     * Sets the position of the commit names
     * on the grid.
     */
    fun setCommitNamesPosition(
        gbc: GridBagConstraints,
        offset: Int,
    ) {
        gbc.gridx = if (alignment == SwingConstants.LEFT) 1 else 0
        gbc.gridy = 1
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        gbc.anchor = GridBagConstraints.CENTER
        gbc.insets = offsetBranchIfAdded(offset)
    }

    /**
     * Sets the position of the branch
     * on the grid.
     */
    fun setBranchPosition(
        gbc: GridBagConstraints,
        offset: Int,
    ) {
        gbc.gridx = if (alignment == SwingConstants.LEFT) 0 else 1
        gbc.gridy = 1
        gbc.weightx = 0.0
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        gbc.insets = offsetBranchIfAdded(offset)
    }

    /**
     * If the branch panel displays an additional branch
     * we offset the branch down.
     */
    private fun offsetBranchIfAdded(offset: Int): Insets = Insets(offset, 5, branchPanel.diameter, 5)

    /**
     * Sets the position of the branch name label
     * on the grid.
     */
    fun setBranchNamePosition(gbc: GridBagConstraints) {
        gbc.gridx = if (alignment == SwingConstants.LEFT) 0 else 1
        gbc.gridy = 0
        gbc.weightx = 0.0
        gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.anchor = GridBagConstraints.SOUTH
        gbc.insets = Insets(branchPanel.diameter, 5, branchPanel.diameter, 5)
    }

    /**
     * Updates branch name
     */
    fun updateBranchName() {
        ((branchNamePanel.getComponent(0) as RoundedPanel).getComponent(0) as BoldLabel).text = branch.name
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

    /**
     * Get a text field by given index
     */
    fun getTextField(i: Int): RoundedTextField {
        val message = messages[i]
        val textWrapper = message.components[1] as JBPanel<*>
        val textField = textWrapper.components[0] as RoundedTextField
        return textField
    }

    /**
     * Alter coloring of text labels
     */
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
