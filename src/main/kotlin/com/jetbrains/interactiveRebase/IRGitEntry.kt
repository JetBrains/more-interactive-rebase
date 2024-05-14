package com.jetbrains.interactiveRebase

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.util.NlsContexts
import com.intellij.vcs.log.VcsCommitMetadata
import git4idea.i18n.GitBundle
import org.jetbrains.annotations.PropertyKey
import java.awt.event.KeyEvent
import java.util.function.Supplier

 open class IRGitEntry(val action: Action, val commit: String, val subject: String)  {

    override fun toString() = "$action $commit $subject"

    sealed class Action(val command: String,
                        val isCommit: Boolean,
                        private val nameKey: @PropertyKey(resourceBundle = GitBundle.BUNDLE) String) {
        object PICK : IRGitEntry.KnownAction("pick", "p", nameKey = "rebase.entry.action.name.pick")
        object EDIT : IRGitEntry.KnownAction("edit", "e", nameKey = "rebase.entry.action.name.edit")
        object DROP : IRGitEntry.KnownAction("drop", "d", nameKey = "rebase.entry.action.name.drop")
        object REWORD : IRGitEntry.KnownAction("reword", "r", nameKey = "rebase.entry.action.name.reword")
        object SQUASH : IRGitEntry.KnownAction("squash", "s", nameKey = "rebase.entry.action.name.squash")
        object FIXUP : IRGitEntry.KnownAction("fixup", "f", nameKey = "rebase.entry.action.name.fixup")
        object UPDATE_REF : IRGitEntry.KnownAction("update-ref", isCommit = false, nameKey = "rebase.entry.action.name.update.ref")

        class Other(command: String) : Action(command, false, nameKey = "rebase.entry.action.name.unknown")

        val visibleName: Supplier<@NlsContexts.Button String> get() = GitBundle.messagePointer(nameKey)

        override fun toString(): String = command
    }


    sealed class KnownAction(command: String,
                              vararg val synonyms: String,
                              isCommit: Boolean = true,
                              nameKey: @PropertyKey(resourceBundle = "messages.GitBundle") String) : Action(command, isCommit, nameKey) {
        val mnemonic: Int get() = KeyEvent.getExtendedKeyCodeForChar(command.first().code)
    }

    companion object {
        @JvmStatic
        fun parseAction(action: String): Action {
            val knownActions = listOf(Action.PICK, Action.EDIT, Action.DROP, Action.REWORD, Action.SQUASH, Action.FIXUP, Action.UPDATE_REF)
            return knownActions.find { it.command == action || it.synonyms.contains(action) } ?: Action.Other(action)
        }
    }
}

//TODO("Maybe we can get rid of IRGitEntryDetails but for now let it be so as to see whether it works")
internal open class IRGitEntryDetails(val entry: IRGitEntry, val commitDetails: VcsCommitMetadata) :
        IRGitEntry(entry.action, entry.commit, entry.subject)

internal fun IRGitEntry.getFullCommitMessage() = (this as? IRGitEntryDetails)?.commitDetails?.fullMessage

@VisibleForTesting
internal class GitRebaseEntryGeneratedUsingLog(details: VcsCommitMetadata) :
        IRGitEntryDetails(IRGitEntry(Action.PICK, details.id.asString(), details.subject.trimStart()), details) {

    fun equalsWithReal(realEntry: IRGitEntry) =
            action == realEntry.action &&
                    commit.startsWith(realEntry.commit) &&
                    subject == realEntry.subject
}