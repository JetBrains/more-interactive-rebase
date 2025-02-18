// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package git4ideaClasses

import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.xmlb.annotations.XCollection
import git4idea.rebase.GitRebaseUtils

/**
 * Used for rewording
 */
@Service(Service.Level.PROJECT)
@State(
    name = "IRRewordedCommitMessages",
    storages = [Storage(value = StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)],
    reportStatistic = false,
)
internal class IRRewordedCommitMessageProvider :
    SimplePersistentStateComponent<GitRewordedCommitMessagesInfo>(GitRewordedCommitMessagesInfo()) {
    companion object {
        @JvmStatic
        fun getInstance(project: Project) = project.service<IRRewordedCommitMessageProvider>()
    }

    fun save(
        project: Project,
        root: VirtualFile,
        mappings: List<RewordedCommitMessageMapping>,
    ) {
        val ontoHash = GitRebaseUtils.getOntoHash(project, root)?.asString() ?: return
        state.onto = ontoHash
        state.currentCommit = 0
        state.commitMessagesMapping.clear()
        state.commitMessagesMapping.addAll(mappings)
    }

    fun getRewordedCommitMessage(
        project: Project,
        root: VirtualFile,
        originalMessage: String,
    ): String? {
        if (!checkRebaseOnto(project, root)) {
            return null
        }
        val commitMappings = state.commitMessagesMapping
        val currentCommit = state.currentCommit.takeIf { it < commitMappings.size } ?: return null
        val mapping = commitMappings[currentCommit]
        val savedOriginalMessage = mapping.originalMessage ?: return null
        val rewordedMessage = mapping.rewordedMessage ?: return null

        return rewordedMessage.takeIf { originalMessage.startsWith(savedOriginalMessage) }?.also {
            state.currentCommit++
        }
    }

    private fun checkRebaseOnto(
        project: Project,
        root: VirtualFile,
    ): Boolean {
        val currentRebaseOntoHash = GitRebaseUtils.getOntoHash(project, root)?.asString() ?: return false
        val savedOntoHash = state.onto
        return currentRebaseOntoHash == savedOntoHash
    }
}

internal class GitRewordedCommitMessagesInfo : BaseState() {
    var onto by string()
    var currentCommit by property(0)

    @get:XCollection
    val commitMessagesMapping by list<RewordedCommitMessageMapping>()
}

internal class RewordedCommitMessageMapping : BaseState() {
    companion object {
        @JvmStatic
        fun fromMapping(
            originalMessage: String,
            rewordedMessage: String,
        ) = RewordedCommitMessageMapping().apply {
            this.originalMessage = originalMessage
            this.rewordedMessage = rewordedMessage
        }
    }

    var originalMessage by string()
    var rewordedMessage by string()
}
