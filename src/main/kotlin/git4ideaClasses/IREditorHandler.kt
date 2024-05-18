// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package git4ideaClasses
import com.intellij.CommonBundle
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vfs.VirtualFile
import git4idea.DialogManager
import git4idea.commands.GitImplBase
import git4idea.config.GitConfigUtil
import git4idea.history.GitLogUtil
import git4idea.i18n.GitBundle
import git4idea.rebase.GitRebaseEditorHandler
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.util.function.Predicate

/**
 * Handler for the rebasing
 */
open class IREditorHandler(private val myProject: Project, private val myRoot: VirtualFile) : GitRebaseEditorHandler {
    /**
     * If interactive rebase editor (with the list of commits) was shown, this is true.
     * In that case, the class expects only unstructured editor to edit the commit message.
     */
    protected var myRebaseEditorShown = false
    private var myCommitListCancelled = false
    private var myUnstructuredEditorCancelled = false
    private val myRewordedCommitMessageProvider: IRRewordedCommitMessageProvider =
        IRRewordedCommitMessageProvider.getInstance(myProject)

    /**
     * Makes the rebase change
     */
    override fun editCommits(file: File): Int {
        return try {
            if (myRebaseEditorShown) {
                val encoding = GitConfigUtil.getCommitEncoding(myProject, myRoot)
                val originalMessage = FileUtil.loadFile(file, encoding)
                println("doesnt get here huh")
                val newMessage =
                    myRewordedCommitMessageProvider.getRewordedCommitMessage(myProject, myRoot, originalMessage)
                println("message created")
                if (newMessage == null) {
                    myUnstructuredEditorCancelled = !handleUnstructuredEditor(file)
                    println("message null")
                    return if (myUnstructuredEditorCancelled) GitRebaseEditorHandler.ERROR_EXIT_CODE else 0
                }
                println("message not null")
                FileUtil.writeToFile(file, newMessage.toByteArray(Charset.forName(encoding)))
                0
            } else {
                setRebaseEditorShown()
                val success = handleInteractiveEditor(file)
                if (success) {
                    0
                } else {
                    println("huhhhhhh")
                    myCommitListCancelled = true
                    GitRebaseEditorHandler.ERROR_EXIT_CODE
                }
            }
        } catch (e: VcsException) {
            LOG.error("Failed to load commit details for commits from git rebase file: $file", e)
            GitRebaseEditorHandler.ERROR_EXIT_CODE
        } catch (e: Exception) {
            LOG.error("Failed to edit git rebase file: $file", e)
            GitRebaseEditorHandler.ERROR_EXIT_CODE
        }
    }

    @Throws(IOException::class)
    protected fun handleUnstructuredEditor(file: File): Boolean {
        return GitImplBase.loadFileAndShowInSimpleEditor(
            myProject,
            myRoot,
            file,
            GitBundle.message("rebase.interactive.edit.commit.message.dialog.title"),
            GitBundle.message("rebase.interactive.edit.commit.message.ok.action.title"),
        )
    }

    @Throws(IOException::class, VcsException::class)
    protected fun handleInteractiveEditor(file: File): Boolean {
        val rebaseFile = IRGitRebaseFile(myProject, myRoot, file)
        return try {
            val entries = rebaseFile.load()
//            if (ContainerUtil.findInstance<Action,
//            IRGitEntry.Action.Other>(ContainerUtil
//            .map<IRGitEntry, Action>(entries, com.intellij.util.Function<IRGitEntry, IRGitEntry.Action>
//            { it: IRGitEntry -> it.action }), IRGitEntry.Action.Other::class.java) != null) {
//                return handleUnstructuredEditor(file)
//            } it used to be this but couldn't make it work
            if (entries.map { it.action }.any { it is IRGitEntry.Action.Other }) {
                return handleUnstructuredEditor(file)
            }

            val newEntries = collectNewEntries(entries)
            if (newEntries != null) {
                rebaseFile.save(newEntries)
                true
            } else {
                rebaseFile.cancel()
                false
            }
        } catch (e: IRGitRebaseFile.NoopException) {
            confirmNoopRebase()
        }
    }

    @Throws(VcsException::class)
    internal open fun collectNewEntries(entries: List<IRGitEntry>): List<IRGitEntry>? {
        val newText = Ref.create<List<IRGitEntry>?>()
        val entriesWithDetails = loadDetailsForEntries(entries)
        ApplicationManager.getApplication().invokeAndWait {
            newText.set(showInteractiveRebaseDialog(entriesWithDetails))
        }
        return newText.get()
    }

    private fun showInteractiveRebaseDialog(entries: List<IRGitEntry>): List<IRGitEntry>? {
        // val editor = GitInteractiveRebaseDialog(myProject, myRoot, entries)
        // DialogManager.show(editor)
        // if (editor.isOK()) {
        val table = IRCommitsTable(entries)
        val rebaseTodoModel = table.rebaseTodoModel
        processModel(rebaseTodoModel)
        return rebaseTodoModel.convertToEntries()
        // it used to be but i changed it  internal fun <T : IRGitEntry> convertToEntries(): List<IRGitEntry>
        // }
        // return null
    }

    protected fun <T : IRGitEntry> processModel(rebaseTodoModel: IRGitModel<T>) {
        processModel(rebaseTodoModel) { entry: IRGitEntry -> (entry as IRGitEntryDetails).commitDetails.fullMessage }
    }

    protected fun <T : IRGitEntry> processModel(
        rebaseTodoModel: IRGitModel<T>,
        fullMessageGetter: (T) -> String,
    ) {
        val messages: MutableList<RewordedCommitMessageMapping> = ArrayList()
        for (element in rebaseTodoModel.elements) {
            if (element.type is IRGitModel.Type.NonUnite.KeepCommit.Reword) {
                messages.add(
                    RewordedCommitMessageMapping.fromMapping(
                        fullMessageGetter.invoke(element.entry),
                        (element.type as IRGitModel.Type.NonUnite.KeepCommit.Reword).newMessage,
                    ),
                )
            }
        }
        myRewordedCommitMessageProvider.save(myProject, myRoot, messages)
    }

    @Throws(VcsException::class)
    private fun loadDetailsForEntries(entries: List<IRGitEntry>): List<IRGitEntry> {
        val commitList =
            entries.stream().filter(
                Predicate<IRGitEntry> {
                        entry: IRGitEntry ->
                    entry.action.isCommit
                },
            ).map<String> { entry: IRGitEntry -> entry.commit }.toList()
        val details = GitLogUtil.collectMetadata(myProject, myRoot, commitList)
        val entriesWithDetails: MutableList<IRGitEntry> = ArrayList()
        var detailsIndex = 0
        for (entry in entries) {
            if (entry.action.isCommit) {
                entriesWithDetails.add(IRGitEntryDetails(entry, details[detailsIndex++]!!))
            } else {
                entriesWithDetails.add(entry)
            }
        }
        return entriesWithDetails
    }

    private fun confirmNoopRebase(): Boolean {
        LOG.info("Noop situation while rebasing $myRoot")
        val result = Ref.create(false)
        ApplicationManager.getApplication().invokeAndWait {
            result.set(
                Messages.OK ==
                    DialogManager.showOkCancelDialog(
                        myProject,
                        GitBundle.message("rebase.interactive.noop.dialog.text"),
                        GitBundle.message("rebase.interactive.noop.dialog.title"),
                        CommonBundle.getOkButtonText(),
                        CommonBundle.getCancelButtonText(),
                        Messages.getQuestionIcon(),
                    ),
            )
        }
        return result.get()
    }

    /**
     * This method is invoked to indicate that this editor will be invoked in the rebase continuation action.
     */
    fun setRebaseEditorShown() {
        myRebaseEditorShown = true
    }

    override fun wasCommitListEditorCancelled(): Boolean {
        return myCommitListCancelled
    }

    override fun wasUnstructuredEditorCancelled(): Boolean {
        return myUnstructuredEditorCancelled
    }

    companion object {
        private val LOG = Logger.getInstance(IREditorHandler::class.java)
    }
}
