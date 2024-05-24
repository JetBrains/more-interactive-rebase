package com.jetbrains.interactiveRebase.service

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.dataClasses.commands.DropCommand
import com.jetbrains.interactiveRebase.dataClasses.commands.PickCommand
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import org.mockito.Mockito

class RebaseInvokerTest : BasePlatformTestCase() {
    fun testAddCommand() {
        val rebaseInvoker = RebaseInvoker(project)
        val dropCommand = Mockito.mock(DropCommand::class.java)
        rebaseInvoker.addCommand(dropCommand)
        assertTrue(rebaseInvoker.commands.size == 1)
    }

    fun testRemoveCommand() {
        val rebaseInvoker = RebaseInvoker(project)
        val pickCommand = Mockito.mock(PickCommand::class.java)
        rebaseInvoker.commands = mutableListOf(pickCommand)
        rebaseInvoker.removeCommand(pickCommand)
        assertTrue(rebaseInvoker.commands.isEmpty())
    }
}
