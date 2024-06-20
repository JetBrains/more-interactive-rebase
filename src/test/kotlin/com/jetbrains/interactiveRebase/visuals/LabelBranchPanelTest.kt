package com.jetbrains.interactiveRebase.visuals

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.util.minimumHeight
import com.intellij.util.ui.JBUI
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.RewordCommand
import com.jetbrains.interactiveRebase.listeners.LabelListener
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.visuals.multipleBranches.RoundedPanel
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.mock
import java.awt.Component.RIGHT_ALIGNMENT
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.SwingConstants

class LabelBranchPanelTest : BasePlatformTestCase() {
    private lateinit var circle: CirclePanel
    private lateinit var labeledBranch: LabeledBranchPanel
    private lateinit var commitProvider: TestGitCommitProvider
    private lateinit var commit1: CommitInfo
    private lateinit var commit2: CommitInfo
    private lateinit var commit3: CommitInfo
    private lateinit var branch: BranchInfo

    override fun setUp() {
        super.setUp()
        commitProvider = TestGitCommitProvider(project)
        circle = mock(CirclePanel::class.java)
        commit1 = CommitInfo(commitProvider.createCommit("One"), project, mutableListOf())
        commit2 = CommitInfo(commitProvider.createCommit("Two"), project, mutableListOf())
        commit3 = CommitInfo(commitProvider.createCommit("Three"), project, mutableListOf())
        branch = BranchInfo("branch", mutableListOf(commit1, commit2, commit3))
        branch.currentCommits = mutableListOf(commit1, commit2, commit3)
        labeledBranch = LabeledBranchPanel(project, branch, Palette.BLUE_THEME)
    }

    fun testGenerateCommitLabel() {
        assertThat(labeledBranch.generateCommitLabel(1, circle).text).isEqualTo("Two")
        assertThat(labeledBranch.generateCommitLabel(1, circle).labelFor).isEqualTo(circle)
        assertThat(labeledBranch.generateCommitLabel(1, circle).verticalTextPosition).isEqualTo(SwingConstants.CENTER)
        assertThat(labeledBranch.generateCommitLabel(1, circle).alignmentX).isEqualTo(JBPanel.LEFT_ALIGNMENT)
    }

    fun testSetBranchNamePosition() {
        val gbc = GridBagConstraints()
        labeledBranch.setBranchNamePosition(gbc)
        assertThat(gbc.gridx).isEqualTo(0)
        assertThat(gbc.gridy).isEqualTo(0)
        assertThat(gbc.weightx).isEqualTo(0.0)
        assertThat(gbc.weighty).isEqualTo(0.0)
        assertThat(gbc.fill).isEqualTo(GridBagConstraints.HORIZONTAL)
        assertThat(gbc.insets).isEqualTo(Insets(30, 5, 30, 5))
    }

    fun testSetBranchPosition() {
        val gbc = GridBagConstraints()
        labeledBranch.setBranchPosition(gbc, 0)
        assertThat(gbc.gridx).isEqualTo(0)
        assertThat(gbc.gridy).isEqualTo(1)
        assertThat(gbc.weightx).isEqualTo(0.0)
        assertThat(gbc.weighty).isEqualTo(1.0)
        assertThat(gbc.fill).isEqualTo(GridBagConstraints.BOTH)
        assertThat(gbc.insets).isEqualTo(Insets(0, 5, 30, 5))
    }

    fun testSetCommitNamesPosition() {
        val gbc = GridBagConstraints()
        labeledBranch.setCommitNamesPosition(gbc, 0)
        assertThat(gbc.gridx).isEqualTo(1)
        assertThat(gbc.gridy).isEqualTo(1)
        assertThat(gbc.weightx).isEqualTo(1.0)
        assertThat(gbc.weighty).isEqualTo(1.0)
        assertThat(gbc.fill).isEqualTo(GridBagConstraints.BOTH)
        assertThat(gbc.anchor).isEqualTo(GridBagConstraints.CENTER)
        assertThat(gbc.insets).isEqualTo(Insets(0, 5, 30, 5))
    }

    fun testGenerateLabelChecksRewordCommand() {
        val rewordChange = RewordCommand(commit1, "new message")
        branch.currentCommits[0].changes.add(rewordChange)
        val label1 = labeledBranch.generateCommitLabel(0, circle)
        assertThat(TextStyle.stripTextFromStyling(label1.text)).isEqualTo("new message")
        val label2 = labeledBranch.generateCommitLabel(1, circle)
        assertThat(TextStyle.stripTextFromStyling(label2.text)).isEqualTo("Two")
    }

    fun testChangeTextWhenCommitDropped() {
        commit3.changes.add(DropCommand(commit3))
        val label = labeledBranch.generateCommitLabel(2, circle)
        assertThat(TextStyle.stripTextFromStyling(label.text)).isEqualTo(commit3.commit.subject)
        assertThat(label.alignmentX).isEqualTo(RIGHT_ALIGNMENT)
        assertThat(label.horizontalAlignment).isEqualTo(SwingConstants.NORTH_EAST)
    }

    fun testCommitLabelIsBold() {
        commit1.isSelected = true
        val label = labeledBranch.generateCommitLabel(0, circle)
        assertThat(label.text).isEqualTo(
            "<html><span style='white-space: nowrap; overflow: hidden; text-overflow: ellipsis;'><b>" +
                commit1.commit.subject + "</b></span></html>",
        )
    }

    fun testWrapsLabelWithTextField() {
        val commitLabel = JBLabel("label")
        val mainWrapper = labeledBranch.wrapLabelWithTextField(commitLabel, commit1)
        val labelWrapper = mainWrapper.getComponent(0) as JBPanel<*>
        val textWrapper = mainWrapper.getComponent(1) as JBPanel<*>

        assertThat(labelWrapper.getComponent(0)).isInstanceOf(JBLabel::class.java)
        assertThat(textWrapper.getComponent(0)).isInstanceOf(RoundedTextField::class.java)
        assertThat(textWrapper.isVisible).isFalse()
        assertThat(labelWrapper.isVisible).isTrue()
        assertThat(labelWrapper.getComponent(0)).isEqualTo(commitLabel)
        assertThat(commitLabel.mouseListeners).hasOnlyOneElementSatisfying { element ->
            assertThat(element).isInstanceOf(LabelListener::class.java)
        }
    }

    fun testWrapsLabelWithTextFieldConsidersDoubleClick() {
        val commitLabel = JBLabel("label")
        commit2.isTextFieldEnabled = true
        val mainWrapper = labeledBranch.wrapLabelWithTextField(commitLabel, commit2)
        val labelWrapper = mainWrapper.getComponent(0) as JBPanel<*>
        val textWrapper = mainWrapper.getComponent(1) as JBPanel<*>

        assertThat(labelWrapper.getComponent(0)).isInstanceOf(JBLabel::class.java)
        assertThat(textWrapper.getComponent(0)).isInstanceOf(RoundedTextField::class.java)
        assertThat(textWrapper.isVisible).isTrue()
        assertThat(labelWrapper.isVisible).isFalse()
        assertThat(labelWrapper.getComponent(0)).isEqualTo(commitLabel)
        assertThat(commitLabel.mouseListeners).hasOnlyOneElementSatisfying { element ->
            assertThat(element).isInstanceOf(LabelListener::class.java)
        }
    }

    fun testCreateTextBoxSetsAlignments() {
        val commitLabel = JBLabel("label")
        val textField = labeledBranch.createTextBox(commitLabel, commit1)
        assertThat(textField.minimumSize).isEqualTo(Dimension(JBUI.scale(300), textField.minimumHeight))
        assertThat(textField.horizontalAlignment).isEqualTo(SwingConstants.LEFT)
    }

    fun testAddComponents() {
        val labelPanelWrapper = labeledBranch.labelPanelWrapper
        labeledBranch.addComponents()
        assertThat(labelPanelWrapper.layout).isInstanceOf(GridBagLayout::class.java)
        assertThat(labelPanelWrapper.getComponent(0)).isInstanceOf(JBPanel::class.java)
        assertThat(labelPanelWrapper.getComponent(1)).isInstanceOf(JBPanel::class.java)
        assertThat(labelPanelWrapper.getComponent(2)).isInstanceOf(JBPanel::class.java)
    }

    fun testAddNotifyAddsComponents() {
        labeledBranch.addNotify()
        assertThat(labeledBranch.layout).isInstanceOf(GridBagLayout::class.java)
        assertThat(labeledBranch.getComponent(0)).isInstanceOf(JBPanel::class.java)
        assertThat(labeledBranch.getComponent(1)).isInstanceOf(BranchPanel::class.java)
        assertThat(labeledBranch.getComponent(2)).isInstanceOf(JBPanel::class.java)
    }
}
