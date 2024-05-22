package com.jetbrains.interactiveRebase.actions.GitPanel

import com.intellij.openapi.actionSystem.*
import com.jetbrains.interactiveRebase.actions.ChangePanel.AddBranchAction
import com.jetbrains.interactiveRebase.actions.ChangePanel.PickAction


class RebaseActionsGroup : DefaultActionGroup() {
//    override fun getChildren(e: AnActionEvent?): Array<AnAction> {
//        return arrayOf(AddBranchAction(), PickAction(), Separator.create(), DropAction(), StopToEditAction(), RewordAction(), SquashAction(), FixupAction())
//    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(event: AnActionEvent) {

        event.presentation.setEnabled(true)
    }

}