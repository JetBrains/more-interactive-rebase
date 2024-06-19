package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBLabel
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.GraphInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.CollapseCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.RewordCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.StopToEditCommand
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.ModelService
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener

class GraphDiffDialogTest : BasePlatformTestCase() {
    private lateinit var dialog: GraphDiffDialog
    private val mouseAdapter: MouseMotionListener = TestMouseMotionListener()
    private val mouseListener: MouseListener = TestMouseListener()
    private val keyListener = TestKeyListener()
    private val focusListener = TestFocusListener()
    private lateinit var provider: TestGitCommitProvider
    private lateinit var commit1: CommitInfo
    private lateinit var commit2: CommitInfo
    private lateinit var commit3: CommitInfo

    override fun setUp() {
        super.setUp()
        this.dialog = GraphDiffDialog(project)
        this.provider = TestGitCommitProvider(project)
        this.commit1 = CommitInfo(provider.createCommit("initial"), project)
        this.commit2 = CommitInfo(provider.createCommit("make readme"), project)
        this.commit3 = CommitInfo(provider.createCommit("add code"), project)
        project.service<ActionService>().mainPanel = MainPanel(project)
    }

    fun testRevertChanges() {
        commit1.isCollapsed = true
        val commandPrimary = CollapseCommand(commit1, mutableListOf(commit2))
        commit1.changes.add(commandPrimary)

        val primaryBranch = BranchInfo("primary", initialCommits = listOf(commit1, commit2))
        primaryBranch.currentCommits = mutableListOf(commit1)

        val commit4 = CommitInfo(provider.createCommit("add tests"), project)
        val commit5 = CommitInfo(provider.createCommit("fix the tests"), project)
        val commit6 = CommitInfo(provider.createCommit("add even more tests"), project)

        val drop1 = DropCommand(commit1)
        commit1.changes.add(drop1)

        val stop2 = StopToEditCommand(commit2)
        commit2.changes.add(stop2)
        val reword2 = RewordCommand(commit2, "new message")
        commit2.changes.add(reword2)

        commit5.isCollapsed = true
        val commandSecondary = CollapseCommand(commit5, mutableListOf(commit6))
        commit5.changes.add(commandSecondary)

        val squash46 = SquashCommand(commit4, mutableListOf(commit6), "new squash")
        commit4.changes.add(squash46)
        commit6.changes.add(squash46)

        val secondaryBranch =
            BranchInfo(
                "secondary",
                initialCommits = listOf(commit4, commit5, commit6),
            )
        secondaryBranch.currentCommits = mutableListOf(commit4, commit5)

        project.service<ModelService>().graphInfo.mainBranch = primaryBranch
        project.service<ModelService>().graphInfo.addedBranch = secondaryBranch

        dialog.revertChangesVisually(project.service<ModelService>().graphInfo)
        assertThat(commit1.changes.isEmpty()).isTrue()
        assertThat(commit2.changes.isEmpty()).isTrue()
        assertThat(commit3.changes.isEmpty()).isTrue()
        assertThat(commit4.changes.isEmpty()).isTrue()
        assertThat(commit5.changes.isEmpty()).isTrue()
        assertThat(commit6.changes.isEmpty()).isTrue()
    }

    fun testDisableAndCreateDisplay() {
        val primaryBranch = BranchInfo("primaryBranch", listOf(commit1, commit2))
        val secondaryBranch = BranchInfo("secondary", listOf(commit3))
        secondaryBranch.baseCommit = commit3

        val graph = GraphInfo(primaryBranch, secondaryBranch)
        val graphPanel = dialog.createGraphDisplay(graph)
        val mainPanel = graphPanel.mainBranchPanel

        assertThat(mainPanel.branchPanel.keyListeners.isEmpty()).isTrue()
        assertThat(mainPanel.branchNamePanel.mouseMotionListeners.isEmpty()).isTrue()
        assertThat(mainPanel.branchNamePanel.mouseListeners.isEmpty()).isTrue()
        assertThat(mainPanel.commitLabels[0].mouseListeners.isEmpty()).isTrue()
        assertThat(mainPanel.keyListeners.isEmpty()).isTrue()
        assertThat(mainPanel.branchPanel.mouseListeners.isEmpty()).isTrue()
        assertThat(mainPanel.branchPanel.circles[0].mouseListeners.isEmpty()).isTrue()

        val secondaryPanel = graphPanel.addedBranchPanel
        assertThat(secondaryPanel).isNotNull()
        assertThat(secondaryPanel?.branchPanel?.keyListeners?.isEmpty()).isTrue()
        assertThat(secondaryPanel?.branchNamePanel?.mouseMotionListeners?.isEmpty()).isTrue()
        assertThat(secondaryPanel?.branchNamePanel?.mouseListeners?.isEmpty()).isTrue()
    }

    fun testExpandBothBranches() {
        commit1.isCollapsed = true
        val commandPrimary = CollapseCommand(commit1, mutableListOf(commit2))
        commit1.changes.add(commandPrimary)
        val primaryBranch = BranchInfo("primary", initialCommits = listOf(commit1, commit2))
        primaryBranch.currentCommits = mutableListOf(commit1)

        val commit4 = CommitInfo(provider.createCommit("add tests"), project)
        val commit5 = CommitInfo(provider.createCommit("fix the tests"), project)
        val commit6 = CommitInfo(provider.createCommit("add even more tests"), project)
        val commit7 = CommitInfo(provider.createCommit("new commit"), project)

        commit5.isCollapsed = true
        val commandSecondary = CollapseCommand(commit5, mutableListOf(commit6, commit7))
        commit5.changes.add(commandSecondary)

        val unrelatedCommand = DropCommand(commit4)
        commit4.changes.add(unrelatedCommand)

        val secondaryBranch =
            BranchInfo(
                "secondary",
                initialCommits = listOf(commit4, commit5, commit6, commit7),
            )
        secondaryBranch.currentCommits = mutableListOf(commit4, commit5)

        val graph = GraphInfo(primaryBranch, secondaryBranch)
        dialog.expandBothBranches(graph)

        assertThat(primaryBranch.currentCommits.size).isEqualTo(2)
        assertThat(commit1.changes.isEmpty()).isTrue()

        assertThat(secondaryBranch.currentCommits.size).isEqualTo(4)
        assertThat(commit5.changes.isEmpty()).isTrue()
        assertThat(commit5.isCollapsed).isFalse()

        assertThat(commit4.changes.size).isEqualTo(1)
        assertThat(commit4.changes.contains(unrelatedCommand)).isTrue()
    }

    fun testRemoveListeners() {
        val label = JBLabel()
        label.addMouseListener(mouseListener)
        label.addKeyListener(keyListener)
        label.addMouseMotionListener(mouseAdapter)
        label.addFocusListener(focusListener)

        dialog.removeListeners(label)
        assertThat(label.keyListeners).isEmpty()
        assertThat(label.mouseListeners).isEmpty()
        assertThat(label.focusListeners).isEmpty()
        assertThat(label.mouseMotionListeners).isEmpty()
    }

    class TestMouseListener : MouseListener {
        override fun mouseClicked(e: MouseEvent?) {}

        override fun mousePressed(e: MouseEvent?) {}

        override fun mouseReleased(e: MouseEvent?) {}

        override fun mouseEntered(e: MouseEvent?) {}

        override fun mouseExited(e: MouseEvent?) {}
    }

    class TestKeyListener : KeyListener {
        override fun keyTyped(e: KeyEvent?) {}

        override fun keyPressed(e: KeyEvent?) {}

        override fun keyReleased(e: KeyEvent?) {}
    }

    class TestFocusListener : FocusListener {
        override fun focusGained(e: FocusEvent?) {}

        override fun focusLost(e: FocusEvent?) {}
    }

    class TestMouseMotionListener : MouseMotionListener {
        override fun mouseDragged(e: MouseEvent?) {}

        override fun mouseMoved(e: MouseEvent?) {}
    }
}
