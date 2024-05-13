package com.jetbrains.interactiveRebase

open class GitRebaseStatus(val type: Type) {
    internal enum class Type {
        /**
         * Rebase has completed successfully.
         */
        SUCCESS,

        /**
         * Rebase started, and some commits were already applied,
         * but then rebase stopped because of conflicts, or to edit during interactive rebase, or because of an error.<br></br>
         * Such rebase can be retried/continued by calling `git rebase --continue/--skip`, or
         * it can be aborted by calling `git rebase --abort`.
         */
        SUSPENDED,

        /**
         * Rebase started, but immediately stopped because of an error at the very beginning.
         * As opposed to [.SUSPENDED], no commits have been applied yet. <br></br>
         * Retrying such rebase requires calling `git rebase <all params>` again,
         * there is nothing to abort.
        </all> */
        ERROR,

        /**
         * Rebase hasn't started yet.
         */
        NOT_STARTED
    }

    override fun toString(): String {
        return type.toString()
    }

    companion object {
        fun notStarted(): GitRebaseStatus {
            return GitRebaseStatus(GitRebaseStatus.Type.NOT_STARTED)
        }
    }
}

