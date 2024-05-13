package com.jetbrains.interactiveRebase

import git4idea.i18n.GitBundle
import org.jetbrains.annotations.Nls


internal object GitSuccessfulRebase : GitRebaseStatus(GitRebaseStatus.Type.SUCCESS) {
    @Nls
    fun formatMessage(currentBranch: String?, baseBranch: String?, withCheckout: Boolean): String {
        return if (withCheckout) {
            GitBundle.message(
                    "rebase.notification.successful.rebased.checkout.message",
                    GitSuccessfulRebase.convertBooleanToInt(currentBranch != null), currentBranch,
                    GitSuccessfulRebase.convertBooleanToInt(baseBranch != null), baseBranch)
        } else {
            GitBundle.message(
                    "rebase.notification.successful.rebased.message",
                    GitSuccessfulRebase.convertBooleanToInt(currentBranch != null), currentBranch,
                    GitSuccessfulRebase.convertBooleanToInt(baseBranch != null), baseBranch)
        }
    }

    private fun convertBooleanToInt(expression: Boolean): Int {
        return if (expression) 1 else 0
    }
}
