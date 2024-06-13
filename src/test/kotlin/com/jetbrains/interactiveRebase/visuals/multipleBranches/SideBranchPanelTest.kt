package com.jetbrains.interactiveRebase.visuals.multipleBranches

import com.intellij.icons.AllIcons
import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.DialogService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import com.jetbrains.interactiveRebase.visuals.Palette
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.awt.GridBagConstraints

class SideBranchPanelTest : BasePlatformTestCase() {
    val branchName = "main"

    fun testSelectBranchTriggersAcceptedWarning() {
        project.service<RebaseInvoker>().commands.clear()
        project.service<RebaseInvoker>().addCommand(
            DropCommand(CommitInfo(TestGitCommitProvider(project).createCommit("add test"), project)),
        )
        val dialog: DialogService = mock(DialogService::class.java)
        `when`(dialog.warningYesNoDialog(anyString(), anyString())).thenReturn(true)
        val modelService = mock(ModelService::class.java)
        val sideBranchPanel = SideBranchPanel(branchName, project, dialog, modelService)

        val result = sideBranchPanel.selectBranch()
        verify(dialog).warningYesNoDialog(
            "Overwriting Changes",
            "Adding another branch to the view will reset the actions you have made. " +
                "Do you want to continue?",
        )
        verify(modelService).addSecondBranchToGraphInfo(branchName)
        assertThat(sideBranchPanel.isOpaque).isTrue()
        assertThat(result).isTrue()
        assertThat(sideBranchPanel.isSelected).isTrue()
        assertThat(sideBranchPanel.backgroundColor).isEqualTo(Palette.JETBRAINS_SELECTED)
    }

    fun testSelectBranchTriggersRejectedWarning() {
        project.service<RebaseInvoker>().commands.clear()
        project.service<RebaseInvoker>().addCommand(
            DropCommand(CommitInfo(TestGitCommitProvider(project).createCommit("add test"), project)),
        )
        val dialog: DialogService = mock(DialogService::class.java)
        `when`(dialog.warningYesNoDialog(anyString(), anyString())).thenReturn(false)
        val modelService = mock(ModelService::class.java)
        val sideBranchPanel = SideBranchPanel(branchName, project, dialog, modelService)

        val result = sideBranchPanel.selectBranch()
        verify(dialog).warningYesNoDialog(
            "Overwriting Changes",
            "Adding another branch to the view will reset the actions you have made. " +
                "Do you want to continue?",
        )
        verify(modelService, never()).addSecondBranchToGraphInfo(branchName)
        assertThat(result).isFalse()
        assertThat(sideBranchPanel.isSelected).isFalse()
        assertThat(sideBranchPanel.backgroundColor).isNotEqualTo(Palette.JETBRAINS_SELECTED)
    }

    fun testDeSelectBranchTriggersRejectedWarning() {
        project.service<RebaseInvoker>().commands.clear()
        val command = DropCommand(CommitInfo(TestGitCommitProvider(project).createCommit("add test"), project))
        project.service<RebaseInvoker>().addCommand(
            command,
        )
        val dialog: DialogService = mock(DialogService::class.java)

        `when`(dialog.warningYesNoDialog(anyString(), anyString())).thenReturn(false)
        val modelService = mock(ModelService::class.java)
        val sideBranchPanel = SideBranchPanel(branchName, project, dialog, modelService)
        sideBranchPanel.isSelected = true
        val result = sideBranchPanel.deselectBranch()
        verify(dialog).warningYesNoDialog(
            "Overwriting Changes",
            "Removing this branch from the view will reset the actions you have made. " +
                "Do you want to continue?",
        )
        verify(modelService, never()).removeSecondBranchFromGraphInfo()
        assertThat(result).isFalse()
        assertThat(sideBranchPanel.isSelected).isTrue()
        assertThat(project.service<RebaseInvoker>().commands).isEqualTo(listOf(command))
    }

    fun testDeSelectBranchTriggersAcceptedWarning() {
        project.service<RebaseInvoker>().commands.clear()
        val command = DropCommand(CommitInfo(TestGitCommitProvider(project).createCommit("add test"), project))
        project.service<RebaseInvoker>().addCommand(
            command,
        )
        val dialog: DialogService = mock(DialogService::class.java)

        `when`(dialog.warningYesNoDialog(anyString(), anyString())).thenReturn(true)
        val modelService = mock(ModelService::class.java)
        val sideBranchPanel = SideBranchPanel(branchName, project, dialog, modelService)
        sideBranchPanel.isSelected = true
        val result = sideBranchPanel.deselectBranch()
        verify(dialog).warningYesNoDialog(
            "Overwriting Changes",
            "Removing this branch from the view will reset the actions you have made. " +
                "Do you want to continue?",
        )
        verify(modelService).removeSecondBranchFromGraphInfo()
        assertThat(result).isTrue()
    }

    fun testDeselectBranchConsidersEmptyChanges() {
        project.service<RebaseInvoker>().commands.clear()
        val dialog: DialogService = mock(DialogService::class.java)
        `when`(dialog.warningYesNoDialog(anyString(), anyString())).thenReturn(false)
        val modelService = mock(ModelService::class.java)
        val sideBranchPanel = SideBranchPanel(branchName, project, dialog, modelService)

        val result = sideBranchPanel.deselectBranch()
        verify(dialog, never()).warningYesNoDialog(anyString(), anyString())
        verify(modelService).removeSecondBranchFromGraphInfo()
        assertThat(result).isTrue()
    }

    fun testSelectBranchConsidersEmptyChanges() {
        project.service<RebaseInvoker>().commands.clear()
        val dialog: DialogService = mock(DialogService::class.java)
        `when`(dialog.warningYesNoDialog(anyString(), anyString())).thenReturn(false)
        val modelService = mock(ModelService::class.java)
        val sideBranchPanel = SideBranchPanel(branchName, project, dialog, modelService)

        val result = sideBranchPanel.selectBranch()
        verify(dialog, never()).warningYesNoDialog(anyString(), anyString())
        verify(modelService).addSecondBranchToGraphInfo(branchName)
        assertThat(result).isTrue()
        assertThat(sideBranchPanel.isSelected).isTrue()
        assertThat(sideBranchPanel.backgroundColor).isEqualTo(Palette.JETBRAINS_SELECTED)
    }

    fun testCreateSideBranchPanel() {
        val sideBranchPanel = SideBranchPanel(branchName, project)
        assertNotNull(sideBranchPanel)
        assertThat(sideBranchPanel.branchName).isEqualTo(branchName)
        assertThat(sideBranchPanel.button).isNotNull
    }

    fun testAddRemoveBranchButton() {
        val sideBranchPanel = SideBranchPanel(branchName, project)
        assertNotNull(sideBranchPanel)
        assertThat(sideBranchPanel.button).isNotNull
        assertThat(sideBranchPanel.button.icon).isEqualTo(AllIcons.General.Remove)
        assertThat(sideBranchPanel.button.arcHeight).isEqualTo(5)
        assertThat(sideBranchPanel.button.arcWidth).isEqualTo(5)
        assertThat(sideBranchPanel.button.isContentAreaFilled).isEqualTo(false)
        assertThat(sideBranchPanel.button.isVisible).isEqualTo(false)
        assertThat(sideBranchPanel.button.border).isNull()
    }

    fun testResetBranchPanelVisually() {
        val sideBranchPanel = SideBranchPanel(branchName, project)
        sideBranchPanel.resetSideBranchPanelVisually()
        assertThat(sideBranchPanel.backgroundColor).isEqualTo(Palette.TRANSPARENT)
        assertThat(sideBranchPanel.label.foreground).isEqualTo(JBColor.BLACK)
        assertThat(sideBranchPanel.button.isVisible).isEqualTo(false)
        assertThat(sideBranchPanel.isSelected).isEqualTo(false)
        assertThat(sideBranchPanel.isOpaque).isEqualTo(false)
    }

    fun testGetAlignmentForButton() {
        val sideBranchPanel = SideBranchPanel(branchName, project)
        val gbc = sideBranchPanel.getAlignmentForButton()
        assertThat(gbc.weightx).isEqualTo(0.1)
        assertThat(gbc.gridx).isEqualTo(1)
        assertThat(gbc.anchor).isEqualTo(GridBagConstraints.LINE_END)
        assertThat(gbc.insets.right).isEqualTo(10)
    }

    fun testOnHover() {
        val sideBranchPanel = SideBranchPanel(branchName, project)
        sideBranchPanel.onHover()
        assertThat(sideBranchPanel.backgroundColor).isEqualTo(Palette.JETBRAINS_HOVER)
    }

    fun testGrayOutText() {
        val sideBranchPanel = SideBranchPanel(branchName, project)
        sideBranchPanel.grayOutText()
        assertThat(sideBranchPanel.label.foreground).isEqualTo(Palette.GRAY_BUTTON)
    }

    fun testButtonOnHover() {
        val sideBranchPanel = SideBranchPanel(branchName, project)
        sideBranchPanel.buttonOnHover()
        assertThat(sideBranchPanel.button.backgroundColor).isEqualTo(sideBranchPanel.background.darker())
    }

    fun testButtonOnHoverExit() {
        val sideBranchPanel = SideBranchPanel(branchName, project)
        sideBranchPanel.buttonOnHoverExit()
        assertThat(sideBranchPanel.button.backgroundColor).isEqualTo(Palette.TRANSPARENT)
    }
}
