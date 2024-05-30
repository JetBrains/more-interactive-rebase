package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.commands.RewordCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.SquashCommand
import com.jetbrains.interactiveRebase.mockStructs.TestGitCommitProvider
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import com.jetbrains.interactiveRebase.services.strategies.SquashTextStrategy
import com.jetbrains.interactiveRebase.visuals.RoundedTextField
import com.jetbrains.rd.framework.base.deepClonePolymorphic
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import java.awt.event.KeyEvent

class TextFieldListenerTest : BasePlatformTestCase() {
    private lateinit var listener: TextFieldListener
    private lateinit var commitInfo1: CommitInfo
    private lateinit var commitInfo2: CommitInfo
    private lateinit var textField: RoundedTextField

    override fun setUp() {
        super.setUp()
        val commitProvider = TestGitCommitProvider(project)
        commitInfo1 = CommitInfo(commitProvider.createCommit("tests"), project, mutableListOf())
        commitInfo2 = CommitInfo(commitProvider.createCommit("fix tests"), project, mutableListOf())
        textField = RoundedTextField(commitInfo1, "input", JBColor.BLUE)
        listener = TextFieldListener(commitInfo1, textField, project.service<RebaseInvoker>())
    }

    fun testKeyReleasedConsidersNull() {
        assertThatCode { listener.keyReleased(null) }.doesNotThrowAnyException()
    }

    fun testKeyReleasedChecksEscape() {
        val key = KeyEvent(JBLabel(), KeyEvent.VK_ESCAPE, 2, 2, KeyEvent.VK_ESCAPE)
        listener.keyReleased(key)
        assertThat(commitInfo1.isTextFieldEnabled).isFalse()
    }

    fun testIsNotAffectedByOtherEvents() {
        val reference = commitInfo1.deepClonePolymorphic()
        val key = KeyEvent(JBLabel(), KeyEvent.VK_ESCAPE, 2, 2, KeyEvent.VK_ESCAPE)
        listener.keyTyped(key)
        listener.keyPressed(key)
        assertThat(commitInfo1).isEqualTo(reference)
    }

    fun testKeyReleasedChecksEnter() {
        listener.keyReleased(KeyEvent(JBLabel(), KeyEvent.VK_ENTER, 2, 2, KeyEvent.VK_ENTER))
        assertThat(commitInfo1.changes).hasOnlyElementsOfType(RewordCommand::class.java)
        assertThat(commitInfo1.changes).hasSize(1)
        val command = commitInfo1.changes[0] as RewordCommand
        assertThat(command.newMessage).isEqualTo(textField.text)
    }

    fun testSetStrategy() {
        val command = SquashCommand(commitInfo1, mutableListOf(commitInfo2), "to be removed")
        val field = RoundedTextField(commitInfo1, "in text field", JBColor.BLUE)

        listener.strategy = SquashTextStrategy(command, field)
        listener.keyReleased(KeyEvent(JBLabel(), KeyEvent.VK_ENTER, 2, 1, KeyEvent.VK_ENTER))
        assertThat(command.newMessage).isEqualTo("in text field")
    }
}
