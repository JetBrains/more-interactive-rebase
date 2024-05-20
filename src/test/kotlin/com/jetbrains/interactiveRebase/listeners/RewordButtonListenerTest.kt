package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.ComponentService
import org.assertj.core.api.Assertions.assertThat

class RewordButtonListenerTest : BasePlatformTestCase() {
    private lateinit var listener: RewordButtonListener
    private lateinit var componentService: ComponentService
    private lateinit var commitInfo1: CommitInfo
    private lateinit var commitInfo2: CommitInfo

    override fun setUp() {
        super.setUp()
        listener = RewordButtonListener(project)
        val commitProvider = TestGitCommitProvider(project)
        commitInfo1 = CommitInfo(commitProvider.createCommit("tests"), project, mutableListOf())
        commitInfo1.isSelected = true
        commitInfo2 = CommitInfo(commitProvider.createCommit("fix tests"), project, mutableListOf())
        componentService = project.service<ComponentService>()
        componentService.branchInfo.selectedCommits.add(commitInfo1)
    }

    fun testActionPerformedSets() {
        listener.actionPerformed(null)
        assertThat(commitInfo1.isDoubleClicked).isTrue()
        assertThat(commitInfo2.isDoubleClicked).isFalse()
        assertThat(componentService.isDirty).isTrue()
    }

    fun testActionPerformedConsidersEmptyList() {
        commitInfo1.isSelected = false
        componentService.branchInfo.selectedCommits.remove(commitInfo1)
        listener.actionPerformed(null)
        assertThat(componentService.isDirty).isFalse()
        assertThat(commitInfo2.isDoubleClicked).isFalse()
        assertThat(commitInfo1.isDoubleClicked).isFalse()
    }
}
