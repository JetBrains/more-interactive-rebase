package com.jetbrains.interactiveRebase.visuals

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import git4idea.GitCommit
import org.assertj.core.api.Assertions.assertThat
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.awt.GridBagConstraints
import java.awt.Insets
import javax.swing.SwingConstants

class LabelBranchPanelTest : BasePlatformTestCase() {
    private lateinit var circle: CirclePanel
    private lateinit var labeledBranch: LabeledBranchPanel

    override fun setUp() {
        super.setUp()
        circle = mock(CirclePanel::class.java)
        val commit1 = CommitInfo(mock(GitCommit::class.java), project, mutableListOf())
        val commit2 = CommitInfo(mock(GitCommit::class.java), project, mutableListOf())
        val commit3 = CommitInfo(mock(GitCommit::class.java), project, mutableListOf())
        `when`(commit1.commit.subject).thenReturn("One")
        `when`(commit2.commit.subject).thenReturn("Two")
        `when`(commit3.commit.subject).thenReturn("Three")
        val branch = BranchInfo("branch", mutableListOf(commit1, commit2, commit3))
        labeledBranch = LabeledBranchPanel(branch, JBColor.BLUE)
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
        assertThat(gbc.weightx).isEqualTo(1.0)
        assertThat(gbc.weighty).isEqualTo(0.0)
        assertThat(gbc.fill).isEqualTo(GridBagConstraints.HORIZONTAL)
        assertThat(gbc.insets).isEqualTo(Insets(5, 5, 5, 5))
    }

    fun testSetBranchPosition() {
        val gbc = GridBagConstraints()
        labeledBranch.setBranchPosition(gbc)
        assertThat(gbc.gridx).isEqualTo(0)
        assertThat(gbc.gridy).isEqualTo(1)
        assertThat(gbc.weightx).isEqualTo(1.0)
        assertThat(gbc.weighty).isEqualTo(1.0)
        assertThat(gbc.fill).isEqualTo(GridBagConstraints.BOTH)
        assertThat(gbc.insets).isEqualTo(Insets(5, 5, 5, 5))
    }

    fun testSetCommitNamesPosition() {
        val gbc = GridBagConstraints()
        labeledBranch.setCommitNamesPosition(gbc)
        assertThat(gbc.gridx).isEqualTo(1)
        assertThat(gbc.gridy).isEqualTo(1)
        assertThat(gbc.weightx).isEqualTo(1.0)
        assertThat(gbc.weighty).isEqualTo(1.0)
        assertThat(gbc.fill).isEqualTo(GridBagConstraints.BOTH)
        assertThat(gbc.insets).isEqualTo(Insets(5, 5, 5, 5))
    }

    fun testWrapCommitLabels() {
        val labelPanelWrapper = JBPanel<JBPanel<*>>()
        labeledBranch.wrapCommitLabels(labelPanelWrapper)
        val label0: JBLabel = labelPanelWrapper.getComponent(0) as JBLabel
        val label1: JBLabel = labelPanelWrapper.getComponent(2) as JBLabel
        val label2: JBLabel = labelPanelWrapper.getComponent(4) as JBLabel
        assertThat(label0.text).isEqualTo("One")
        assertThat(label1.text).isEqualTo("Two")
        assertThat(label2.text).isEqualTo("Three")
    }
}
