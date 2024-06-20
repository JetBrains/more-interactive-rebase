package com.jetbrains.interactiveRebase.actions

import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.testFramework.TestActionEvent.createTestEvent
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.interactiveRebase.actions.buttonActions.AbortRebaseAction
import com.jetbrains.interactiveRebase.actions.buttonActions.ContinueRebaseAction
import com.jetbrains.interactiveRebase.actions.changePanel.AddBranchAction
import com.jetbrains.interactiveRebase.actions.changePanel.CollapseAction
import com.jetbrains.interactiveRebase.actions.changePanel.RedoAction
import com.jetbrains.interactiveRebase.actions.changePanel.UndoAction
import com.jetbrains.interactiveRebase.actions.changePanel.ViewDiffAction
import com.jetbrains.interactiveRebase.actions.gitPanel.DropAction
import com.jetbrains.interactiveRebase.actions.gitPanel.FixupAction
import com.jetbrains.interactiveRebase.actions.gitPanel.PickAction
import com.jetbrains.interactiveRebase.actions.gitPanel.RebaseAction
import com.jetbrains.interactiveRebase.actions.gitPanel.RewordAction
import com.jetbrains.interactiveRebase.actions.gitPanel.SquashAction
import com.jetbrains.interactiveRebase.actions.gitPanel.StopToEditAction
import org.assertj.core.api.Assertions.assertThat

class TooltipTests : BasePlatformTestCase(){

    fun testRedoTooltip(){
        val redoAction = RedoAction()
        val redoTestEvent = createTestEvent(redoAction)
        val tooltip = redoAction.createCustomComponent(redoTestEvent.presentation, "test")
        assertThat(tooltip).isInstanceOf(ActionButton::class.java)
    }

    fun testUndoTooltip(){
        val undoAction = UndoAction()
        val redoTestEvent = createTestEvent(undoAction)
        val tooltip = undoAction.createCustomComponent(redoTestEvent.presentation, "test")
        assertThat(tooltip).isInstanceOf(ActionButton::class.java)
    }

    fun testRewordTooltip(){
        val rewordAction = RewordAction()
        val rewordTestEvent = createTestEvent(rewordAction)
        val tooltip = rewordAction.createCustomComponent(rewordTestEvent.presentation, "test")
        assertThat(tooltip).isInstanceOf(ActionButton::class.java)
    }

    fun testStopToEditTooltip(){
        val stopToEditAction = StopToEditAction()
        val stopToEditTestEvent = createTestEvent(stopToEditAction)
        val tooltip = stopToEditAction.createCustomComponent(stopToEditTestEvent.presentation, "test")
        assertThat(tooltip).isInstanceOf(ActionButton::class.java)
    }

    fun testSquashTooltip(){
        val squashAction = SquashAction()
        val squashTestEvent = createTestEvent(squashAction)
        val tooltip = squashAction.createCustomComponent(squashTestEvent.presentation, "test")
        assertThat(tooltip).isInstanceOf(ActionButton::class.java)
    }

    fun testFixupTooltip(){
        val fixupAction = FixupAction()
        val fixupTestEvent = createTestEvent(fixupAction)
        val tooltip = fixupAction.createCustomComponent(fixupTestEvent.presentation, "test")
        assertThat(tooltip).isInstanceOf(ActionButton::class.java)
    }

    fun testDropTooltip(){
        val dropAction = DropAction()
        val dropTestEvent = createTestEvent(dropAction)
        val tooltip = dropAction.createCustomComponent(dropTestEvent.presentation, "test")
        assertThat(tooltip).isInstanceOf(ActionButton::class.java)
    }

    fun testPickTooltip(){
        val pickAction = PickAction()
        val pickTestEvent = createTestEvent(pickAction)
        val tooltip = pickAction.createCustomComponent(pickTestEvent.presentation, "test")
        assertThat(tooltip).isInstanceOf(ActionButton::class.java)
    }

    fun testContinueTooltip(){
        val continueAction = ContinueRebaseAction()
        val continueTestEvent = createTestEvent(continueAction)
        val tooltip = continueAction.createCustomComponent(continueTestEvent.presentation, "test")
        assertThat(tooltip).isInstanceOf(ActionButton::class.java)
    }

    fun testAbortTooltip(){
        val abortAction = AbortRebaseAction()
        val abortTestEvent = createTestEvent(abortAction)
        val tooltip = abortAction.createCustomComponent(abortTestEvent.presentation, "test")
        assertThat(tooltip).isInstanceOf(ActionButton::class.java)
    }

    fun testCollapseTooltip(){
        val collapseAction = CollapseAction()
        val collapseTestEvent = createTestEvent(collapseAction)
        val tooltip = collapseAction.createCustomComponent(collapseTestEvent.presentation, "test")
        assertThat(tooltip).isInstanceOf(ActionButton::class.java)
    }

    fun testViewDiffTooltip(){
        val viewDiffAction = ViewDiffAction()
        val viewDiffTestEvent = createTestEvent(viewDiffAction)
        val tooltip = viewDiffAction.createCustomComponent(viewDiffTestEvent.presentation, "test")
        assertThat(tooltip).isInstanceOf(ActionButton::class.java)
    }

    fun testRebaseTooltip(){
        val rebaseAction = RebaseAction()
        val rebaseTestEvent = createTestEvent(rebaseAction)
        val tooltip = rebaseAction.createCustomComponent(rebaseTestEvent.presentation, "test")
        assertThat(tooltip).isInstanceOf(ActionButton::class.java)
    }

    fun testAddBranchTooltip(){
        val addBranchAction = AddBranchAction()
        val addBranchTestEvent = createTestEvent(addBranchAction)
        val tooltip = addBranchAction.createCustomComponent(addBranchTestEvent.presentation, "test")
        assertThat(tooltip).isInstanceOf(ActionButton::class.java)
    }
}