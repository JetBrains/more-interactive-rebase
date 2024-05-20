package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBLabel
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.ComponentService
import org.assertj.core.api.Assertions.assertThat
import java.awt.event.MouseEvent

class LabelListenerTest : BasePlatformTestCase() {
    private lateinit var listener: LabelListener
    private lateinit var commitInfo: CommitInfo
    private lateinit var componentService: ComponentService

    override fun setUp() {
        super.setUp()
        val commitProvider = TestGitCommitProvider(project)
        commitInfo = CommitInfo(commitProvider.createCommit("fix tests"), project, mutableListOf())
        listener = LabelListener(commitInfo)
        componentService = project.service<ComponentService>()
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
        assertThat(componentService.isDirty).isTrue()
        assertThat(componentService.branchInfo.selectedCommits).contains(commitInfo)
    }

    fun testMouseOneClickDeselect() {
        val event = MouseEvent(JBLabel(), 2, 2, 2, 2, 2, 1, false)
        commitInfo.isSelected = true
        componentService.branchInfo.selectedCommits.add(commitInfo)
        listener.mouseClicked(event)
        assertThat(commitInfo.isDoubleClicked).isFalse()
        assertThat(commitInfo.isSelected).isFalse()
        assertThat(componentService.isDirty).isTrue()
        assertThat(componentService.branchInfo.selectedCommits).doesNotContain(commitInfo)
    }

    fun testDoubleClickSelects() {
        val event = MouseEvent(JBLabel(), 2, 2, 2, 2, 2, 2, false)
        listener.mouseClicked(event)
        assertThat(commitInfo.isDoubleClicked).isTrue()
        assertThat(commitInfo.isSelected).isTrue()
        assertThat(componentService.branchInfo.selectedCommits).contains(commitInfo)
    }

    fun testDoubleClickDoesntDeselect() {
        commitInfo.isSelected = true
        componentService.branchInfo.selectedCommits.add(commitInfo)
        val event = MouseEvent(JBLabel(), 2, 2, 2, 2, 2, 2, false)
        listener.mouseClicked(event)
        assertThat(commitInfo.isDoubleClicked).isTrue()
        assertThat(commitInfo.isSelected).isTrue()
    }

    fun testMouseEnteredHovers() {
        listener.mouseEntered(null)
        assertThat(commitInfo.isHovered).isTrue()
        assertThat(componentService.isDirty).isTrue()
    }

    fun testMouseExitUnHovers() {
        listener.mouseExited(null)
        assertThat(commitInfo.isHovered).isFalse()
        assertThat(componentService.isDirty).isTrue()
    }
}
