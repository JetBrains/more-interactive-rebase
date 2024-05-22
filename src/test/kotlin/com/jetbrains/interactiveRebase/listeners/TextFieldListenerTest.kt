package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.RewordCommand
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import com.jetbrains.interactiveRebase.visuals.RoundedTextField
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import java.awt.event.KeyEvent

class TextFieldListenerTest : BasePlatformTestCase() {
    private lateinit var listener: TextFieldListener
    private lateinit var commitInfo: CommitInfo
    private lateinit var textField: RoundedTextField

    override fun setUp() {
        super.setUp()
        val commitProvider = TestGitCommitProvider(project)
        commitInfo = CommitInfo(commitProvider.createCommit("tests"), project, mutableListOf())
        textField = RoundedTextField(commitInfo, "input", JBColor.BLUE)
        listener = TextFieldListener(commitInfo, textField, project.service<RebaseInvoker>())
    }

    fun testKeyReleasedConsidersNull() {
        assertThatCode { listener.keyReleased(null) }.doesNotThrowAnyException()
    }

    fun testKeyReleasedChecksEscape() {
        val key = KeyEvent(JBLabel(), KeyEvent.VK_ESCAPE, 2, 2, KeyEvent.VK_ESCAPE)
        listener.keyReleased(key)
        assertThat(commitInfo.isDoubleClicked).isFalse()
    }

    fun testIsNotAffectedByOtherEvents() {
        val key = KeyEvent(JBLabel(), KeyEvent.VK_ESCAPE, 2, 2, KeyEvent.VK_ESCAPE)
        listener.keyTyped(key)
        assertThat(commitInfo).isEqualTo(commitInfo)
        listener.keyPressed(key)
        assertThat(commitInfo).isEqualTo(commitInfo)
    }

    fun testKeyReleasedChecksEnter() {
        listener.keyReleased(KeyEvent(JBLabel(), KeyEvent.VK_ENTER, 2, 2, KeyEvent.VK_ENTER))
        assertThat(commitInfo.changes).hasOnlyElementsOfType(RewordCommand::class.java)
        assertThat(commitInfo.changes).hasSize(1)
        val command = commitInfo.changes[0] as RewordCommand
        assertThat(command.newMessage).isEqualTo(textField.text)
    }
}
