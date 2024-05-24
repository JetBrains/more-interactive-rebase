package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.ModelService
import org.assertj.core.api.Assertions.assertThat

class RewordButtonListenerTest : BasePlatformTestCase() {
    private lateinit var listener: RewordButtonListener
    private lateinit var modelService: ModelService
    private lateinit var commitInfo1: CommitInfo
    private lateinit var commitInfo2: CommitInfo

    override fun setUp() {
        super.setUp()
        listener = RewordButtonListener(project)
        val commitProvider = TestGitCommitProvider(project)
        commitInfo1 = CommitInfo(commitProvider.createCommit("tests"), project, mutableListOf())
        commitInfo1.isSelected = true
        commitInfo2 = CommitInfo(commitProvider.createCommit("fix tests"), project, mutableListOf())
        modelService = project.service<ModelService>()
        modelService.branchInfo.addSelectedCommits(commitInfo1)
    }

    fun testActionPerformedSets() {
        listener.actionPerformed(null)
        assertThat(commitInfo1.isDoubleClicked).isTrue()
        assertThat(commitInfo2.isDoubleClicked).isFalse()
    }

    fun testActionPerformedConsidersEmptyList() {
        commitInfo1.isSelected = false
        modelService.branchInfo.clearSelectedCommits()
        listener.actionPerformed(null)
        assertThat(commitInfo2.isDoubleClicked).isFalse()
        assertThat(commitInfo1.isDoubleClicked).isFalse()
    }
}
