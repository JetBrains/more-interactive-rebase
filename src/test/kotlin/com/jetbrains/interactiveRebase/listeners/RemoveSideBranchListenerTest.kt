package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.GraphInfo
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import com.jetbrains.interactiveRebase.visuals.GraphPanel
import com.jetbrains.interactiveRebase.visuals.MainPanel
import com.jetbrains.interactiveRebase.visuals.Palette
import com.jetbrains.interactiveRebase.visuals.multipleBranches.SideBranchPanel
import com.jetbrains.interactiveRebase.visuals.multipleBranches.SidePanel
import org.assertj.core.api.Assertions.assertThat
import java.awt.event.MouseEvent

class RemoveSideBranchListenerTest : BasePlatformTestCase() {
    lateinit var sideBranchPanel: SideBranchPanel
    lateinit var parent: SidePanel
    lateinit var removeSideBranchListener: RemoveSideBranchListener
    lateinit var mouseEvent: MouseEvent

    override fun setUp() {
        super.setUp()
        val mainPanel = MainPanel(project)
        mainPanel.graphPanel = GraphPanel(project)
        project.service<ActionService>().mainPanel = mainPanel
        val commit = CommitInfo(TestGitCommitProvider(project).createCommit("commit1"), project)
        project.service<ModelService>().graphInfo =
            GraphInfo(
                BranchInfo(
                    "",
                    mutableListOf(),
                    mutableListOf(commit),
                ),
                BranchInfo(),
            )
        sideBranchPanel = SideBranchPanel("main", project)
        parent = SidePanel(mutableListOf("feature", "bugfix"), project)
        parent.sideBranchPanels.add(sideBranchPanel)
        parent.sideBranchPanels.add(SideBranchPanel("feature", project))
        parent.sideBranchPanels.add(SideBranchPanel("bugfix", project))
        removeSideBranchListener = RemoveSideBranchListener(sideBranchPanel, parent)
        mouseEvent = MouseEvent(sideBranchPanel, 0, 0, 0, 0, 0, 0, false)
    }

    fun testMouseClickedBranchSelected() {
        project.service<RebaseInvoker>().commands.clear()
        sideBranchPanel.isSelected = true
        removeSideBranchListener.mouseClicked(mouseEvent)
        assertThat(sideBranchPanel.isSelected).isFalse()
        assertThat(sideBranchPanel.backgroundColor).isEqualTo(Palette.TRANSPARENT)
    }

    fun testMouseOnHover() {
        removeSideBranchListener.mouseEntered(mouseEvent)
        assertThat(sideBranchPanel.button.backgroundColor).isEqualTo(sideBranchPanel.button.backgroundColor)
    }

    fun testMouseOnHoverExit() {
        removeSideBranchListener.mouseExited(mouseEvent)
        assertThat(sideBranchPanel.button.backgroundColor).isEqualTo(Palette.TRANSPARENT)
    }
}
