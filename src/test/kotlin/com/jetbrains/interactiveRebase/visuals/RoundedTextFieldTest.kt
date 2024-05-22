package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.JBColor
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.ModelService
import org.assertj.core.api.Assertions.assertThat

class RoundedTextFieldTest : BasePlatformTestCase() {
    private lateinit var textField: RoundedTextField
    private lateinit var commitInfo: CommitInfo
    private lateinit var modelService: ModelService

    override fun setUp() {
        super.setUp()
        val commitProvider = TestGitCommitProvider(project)
        commitInfo = CommitInfo(commitProvider.createCommit("fix tests"), project, mutableListOf())
        textField = RoundedTextField(commitInfo, "input", JBColor.BLUE)
        modelService = project.service<ModelService>()
    }

    fun testExitTextBoxExits() {
        textField.exitTextBox()
        assertThat(commitInfo.isDoubleClicked).isFalse()
        assertThat(modelService.branchInfo.selectedCommits).doesNotContain(commitInfo)
        assertThat(commitInfo.isSelected).isFalse()
    }

    fun testRoundedBorderPaintsRightAttributes() {
        val border = textField.border
        assertThat(border).isInstanceOf(RoundedBorder::class.java)
        assertThat(border.isBorderOpaque).isFalse()
    }
}
