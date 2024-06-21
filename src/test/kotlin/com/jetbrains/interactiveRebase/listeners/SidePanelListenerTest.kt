package com.jetbrains.interactiveRebase.listeners

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import com.jetbrains.interactiveRebase.visuals.multipleBranches.SideBranchPanel
import com.jetbrains.interactiveRebase.visuals.multipleBranches.SidePanel
import org.assertj.core.api.Assertions.assertThat
import java.awt.event.KeyEvent

class SidePanelListenerTest : BasePlatformTestCase(){
    lateinit var sidePanel: SidePanel
    lateinit var sideBranchPanel1: SideBranchPanel
    lateinit var sideBranchPanel2: SideBranchPanel
    lateinit var listener: SidePanelListener

    lateinit var upEvent:KeyEvent
    lateinit var downEvent : KeyEvent
    lateinit var rightEvent : KeyEvent
    lateinit var enterEvent : KeyEvent
    lateinit var escapeEvent : KeyEvent

    override fun setUp() {
        super.setUp()
        sidePanel = SidePanel(mutableListOf("test", "twoTest"), project)
        sideBranchPanel1 = SideBranchPanel("test", project)
        sideBranchPanel2 = SideBranchPanel("twoTest", project)

        sidePanel.sideBranchPanels = mutableListOf(sideBranchPanel1, sideBranchPanel2)
        listener = SidePanelListener(project, sidePanel)
        upEvent =
            KeyEvent(
                sidePanel,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                KeyEvent.SHIFT_DOWN_MASK,
                KeyEvent.VK_UP,
                KeyEvent.CHAR_UNDEFINED,
            )
        downEvent =
            KeyEvent(
                sidePanel,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                KeyEvent.SHIFT_DOWN_MASK,
                KeyEvent.VK_DOWN,
                KeyEvent.CHAR_UNDEFINED,
            )
        rightEvent =
            KeyEvent(
                sidePanel,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                KeyEvent.SHIFT_DOWN_MASK,
                KeyEvent.VK_RIGHT,
                KeyEvent.CHAR_UNDEFINED,
            )
        enterEvent =
            KeyEvent(
                sidePanel,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                KeyEvent.SHIFT_DOWN_MASK,
                KeyEvent.VK_ENTER,
                KeyEvent.CHAR_UNDEFINED,
            )

        escapeEvent =
            KeyEvent(
                sidePanel,
                KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                KeyEvent.SHIFT_DOWN_MASK,
                KeyEvent.VK_ESCAPE,
                KeyEvent.CHAR_UNDEFINED,
            )
    }

    fun testUp(){
        listener.selected = sideBranchPanel2
        sideBranchPanel2.isSelected = true
        listener.keyPressed(upEvent)
        assertThat(sideBranchPanel2.isSelected).isTrue()

        sideBranchPanel2.isSelected = false
        listener.keyPressed(upEvent)
        assertThat(listener.selected).isEqualTo(sideBranchPanel1)
        assertThat(sideBranchPanel1.isHovered).isTrue()
    }

    fun testDown(){
        listener.selected = sideBranchPanel1
        sideBranchPanel1.isSelected = true
        listener.keyPressed(downEvent)
        assertThat(sideBranchPanel1.isSelected).isTrue()

        sideBranchPanel1.isSelected = false
        listener.keyPressed(downEvent)
        assertThat(listener.selected).isEqualTo(sideBranchPanel2)
        assertThat(sideBranchPanel2.isHovered).isTrue()
    }

    fun testEnter(){
        project.service<RebaseInvoker>().commands.clear()
        listener.selected = sideBranchPanel1
        sideBranchPanel1.isSelected = true
        listener.keyPressed(enterEvent)
        assertThat(sideBranchPanel1.isSelected).isTrue()

        listener.selected = sideBranchPanel1
        sideBranchPanel1.isSelected = false
        listener.keyPressed(enterEvent)
        assertThat(sideBranchPanel1.isSelected).isTrue()
    }

    fun testEscape(){
        project.service<RebaseInvoker>().commands.clear()
        listener.selected = sideBranchPanel1
        sideBranchPanel1.isSelected = false
        listener.keyPressed(escapeEvent)
        assertThat(sideBranchPanel1.isSelected).isFalse()

        listener.selected = sideBranchPanel1
        sideBranchPanel1.isSelected = true
        listener.keyPressed(escapeEvent)
        assertThat(sideBranchPanel1.isSelected).isFalse()
        assertThat(sideBranchPanel1.isHovered).isTrue()
    }

    fun testMethodsWithEmptyBody(){
        listener.keyPressed(rightEvent)
        val emptyListener = SidePanelListener(project, sidePanel)
        emptyListener.keyTyped(upEvent)
        emptyListener.keyReleased(upEvent)
        assertThat(emptyListener.selected).isNull()
        emptyListener.dispose()
    }

    fun testNullEvent(){
        listener.selected = null
        listener.keyPressed(null)
        assertThat(listener.selected).isNull()
    }

}