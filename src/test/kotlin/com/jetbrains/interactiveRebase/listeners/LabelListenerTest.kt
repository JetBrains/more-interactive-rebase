package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBLabel
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.ModelService
import org.assertj.core.api.Assertions.assertThat
import java.awt.event.MouseEvent

class LabelListenerTest : BasePlatformTestCase() {
    private lateinit var listener: LabelListener
    private lateinit var commitInfo: CommitInfo
    private lateinit var modelService: ModelService

    override fun setUp() {
        super.setUp()
        val commitProvider = TestGitCommitProvider(project)
        commitInfo = CommitInfo(commitProvider.createCommit("fix tests"), project, mutableListOf())
        listener = LabelListener(commitInfo)
        modelService = project.service<ModelService>()
    }

    fun testMouseClickedConsidersNull() {
        listener.mouseClicked(null)
        assertThat(commitInfo.isSelected).isFalse()
        assertThat(commitInfo.isDoubleClicked).isFalse()
    }

    fun testMouseClickedConsidersOneClickSelect() {
        val event = MouseEvent(JBLabel(), 2, 2, 2, 2, 2, 1, false)
        listener.mouseClicked(event)
        assertThat(commitInfo.isDoubleClicked).isFalse()
        assertThat(commitInfo.isSelected).isTrue()
        assertThat(modelService.branchInfo.selectedCommits).contains(commitInfo)
    }

    fun testMouseOneClickDeselect() {
        val event = MouseEvent(JBLabel(), 2, 2, 2, 2, 2, 1, false)
        commitInfo.isSelected = true
        modelService.branchInfo.addSelectedCommits(commitInfo)
        listener.mouseClicked(event)
        assertThat(commitInfo.isDoubleClicked).isFalse()
        assertThat(commitInfo.isSelected).isFalse()
        assertThat(modelService.branchInfo.selectedCommits).doesNotContain(commitInfo)
    }

    fun testDoubleClickSelects() {
        val event = MouseEvent(JBLabel(), 2, 2, 2, 2, 2, 2, false)
        listener.mouseClicked(event)
        assertThat(commitInfo.isDoubleClicked).isTrue()
        assertThat(commitInfo.isSelected).isTrue()
        assertThat(modelService.branchInfo.selectedCommits).contains(commitInfo)
    }

    fun testDoubleClickDoesntDeselect() {
        commitInfo.isSelected = true
        modelService.branchInfo.selectedCommits.add(commitInfo)
        val event = MouseEvent(JBLabel(), 2, 2, 2, 2, 2, 2, false)
        listener.mouseClicked(event)
        assertThat(commitInfo.isDoubleClicked).isTrue()
        assertThat(commitInfo.isSelected).isTrue()
    }
}
