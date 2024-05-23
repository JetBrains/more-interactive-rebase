// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package git4ideaClasses

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vcs.VcsException
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitUtil
import git4idea.config.GitConfigUtil
import git4idea.config.GitVersionSpecialty
import git4idea.util.StringScanner
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.PrintWriter

internal class IRGitRebaseFile(private val myProject: Project, private val myRoot: VirtualFile, private val myFile: File) {
    @kotlin.jvm.Throws(NoopException::class, IOException::class, VcsException::class)
    fun load(): List<IRGitEntry> {
        val encoding = GitConfigUtil.getLogEncoding(myProject, myRoot)
        val entries: MutableList<IRGitEntry> = ArrayList()
        val s = StringScanner(FileUtil.loadFile(myFile, encoding))
        var noop = false
        while (s.hasMoreData()) {
            if (s.isEol || isComment(s)) {
                s.nextLine()
                continue
            }
            if (s.startsWith("noop")) {
                noop = true
                s.nextLine()
                continue
            }
            val command = s.spaceToken()
            val hash = s.spaceToken()
            val comment = s.line()
            val action: IRGitEntry.Action = IRGitEntry.parseAction(command)
            entries.add(IRGitEntry(action, hash, comment))
        }
        if (noop && entries.isEmpty()) {
            throw NoopException()
        }
        return entries
    }

    private fun isComment(s: StringScanner): Boolean {
        val commentChar = if (GitVersionSpecialty.KNOWS_CORE_COMMENT_CHAR.existsIn(myProject)) GitUtil.COMMENT_CHAR else "#"
        return s.startsWith(commentChar)
    }

    @Throws(IOException::class)
    fun cancel() {
        PrintWriter(FileWriter(myFile)).use { out -> out.println("# rebase is cancelled") }
    }

    @Throws(IOException::class)
    fun save(entries: List<IRGitEntry>) {
        val encoding = GitConfigUtil.getLogEncoding(myProject, myRoot)
        PrintWriter(OutputStreamWriter(FileOutputStream(myFile), encoding)).use { out ->
            val knowsDropAction = GitVersionSpecialty.KNOWS_REBASE_DROP_ACTION.existsIn(myProject)
            for (e in entries) {
                if (e.action !== IRGitEntry.Action.DROP || knowsDropAction) {
                    out.println(e)
                }
            }
        }
    }

    internal class NoopException : Exception()
}
