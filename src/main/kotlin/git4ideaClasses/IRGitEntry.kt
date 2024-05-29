// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package git4ideaClasses

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.util.NlsContexts
import com.intellij.vcs.log.VcsCommitMetadata
import git4idea.i18n.GitBundle
import org.jetbrains.annotations.PropertyKey
import java.awt.event.KeyEvent
import java.util.function.Supplier

/**
 * Entry for the action to be executed
 */
open class IRGitEntry(val action: Action, val commit: String, val subject: String) {
    /**
     * String representation of the entry
     */
    override fun toString() = "$action $commit $subject"

    /**
     * Class for the possible actions
     */
    sealed class Action(
        val command: String,
        val isCommit: Boolean,
        private val nameKey:
            @PropertyKey(resourceBundle = GitBundle.BUNDLE)
            String,
    ) {
        object PICK : KnownAction("pick", "p", nameKey = "rebase.entry.action.name.pick")

        object EDIT : KnownAction("edit", "e", nameKey = "rebase.entry.action.name.edit")

        object DROP : KnownAction("drop", "d", nameKey = "rebase.entry.action.name.drop")

        object REWORD : KnownAction("reword", "r", nameKey = "rebase.entry.action.name.reword")

        object SQUASH : KnownAction("squash", "s", nameKey = "rebase.entry.action.name.squash")

        object FIXUP : KnownAction("fixup", "f", nameKey = "rebase.entry.action.name.fixup")

        object UPDATEREF : KnownAction(
            "update-ref",
            isCommit = false,
            nameKey = "rebase.entry.action.name.update.ref",
        )

        /**
         * Other action for a command for the Terminal
         */
        class Other(command: String) : Action(command, false, nameKey = "rebase.entry.action.name.unknown")

        val visibleName: Supplier<@NlsContexts.Button String> get() = GitBundle.messagePointer(nameKey)

        /**
         * The Terminal command
         */
        override fun toString(): String = command
    }

    sealed class KnownAction(
        command: String,
        vararg val synonyms: String,
        isCommit: Boolean = true,
        nameKey:
            @PropertyKey(resourceBundle = "messages.GitBundle")
            String,
    ) :
        Action(command, isCommit, nameKey) {
        val mnemonic: Int get() = KeyEvent.getExtendedKeyCodeForChar(command.first().code)
    }

    companion object {
        @JvmStatic
        fun parseAction(action: String): Action {
            val knownActions =
                listOf(
                    Action.PICK,
                    Action.EDIT,
                    Action.DROP,
                    Action.REWORD,
                    Action.SQUASH,
                    Action.FIXUP,
                    Action.UPDATEREF,
                )
            return knownActions.find { it.command == action || it.synonyms.contains(action) } ?: Action.Other(action)
        }
    }
}

/**
 * Used for translating adding the commit details to the entry
 */
internal open class IRGitEntryDetails(val entry: IRGitEntry, val commitDetails: VcsCommitMetadata) :
    IRGitEntry(entry.action, entry.commit, entry.subject)

/**
 * Retrieves the full commit message
 */
internal fun IRGitEntry.getFullCommitMessage() = (this as? IRGitEntryDetails)?.commitDetails?.fullMessage

/**
 * Translates a commit to an entry for the initial table
 */
@VisibleForTesting
internal class GitRebaseEntryGeneratedUsingLog(val details: VcsCommitMetadata) :
    IRGitEntryDetails(IRGitEntry(Action.PICK, details.id.asString().trimStart(), details.subject.trimStart()), details) {
    override fun equals(other: Any?): Boolean {
        if (other != null) {
            if (other is GitRebaseEntryGeneratedUsingLog) {
                val that = other as GitRebaseEntryGeneratedUsingLog
                return this.details.equals(other.details)
            }
            return false
        }
        return false
    }
}
