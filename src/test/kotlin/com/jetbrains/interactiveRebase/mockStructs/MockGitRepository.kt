package com.jetbrains.interactiveRebase.mockStructs

import com.intellij.dvcs.repo.Repository
import com.intellij.mock.MockVirtualFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitLocalBranch
import git4idea.GitVcs
import git4idea.branch.GitBranchesCollection
import git4idea.ignore.GitRepositoryIgnoredFilesHolder
import git4idea.repo.GitBranchTrackInfo
import git4idea.repo.GitRemote
import git4idea.repo.GitRepoInfo
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryFiles
import git4idea.repo.GitSubmoduleInfo
import git4idea.repo.GitUntrackedFilesHolder
import git4idea.status.GitStagingAreaHolder

@Suppress("removal")
class MockGitRepository(private val branchName: String?) : GitRepository {
    override fun dispose() {
        throw UnsupportedOperationException()
    }

    override fun getRoot(): VirtualFile {
        return MockVirtualFile("mockFile")
    }

    override fun getPresentableUrl(): String {
        throw UnsupportedOperationException()
    }

    override fun getProject(): Project {
        throw UnsupportedOperationException()
    }

    override fun getState(): Repository.State {
        throw UnsupportedOperationException()
    }

    override fun getCurrentBranchName(): String? {
        return branchName
    }

    override fun getVcs(): GitVcs {
        throw UnsupportedOperationException()
    }

    override fun getCurrentRevision(): String? {
        return branchName
    }

    override fun isFresh(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun update() {
        throw UnsupportedOperationException()
    }

    override fun toLogString(): String {
        throw UnsupportedOperationException()
    }

    @Deprecated("Deprecated in Java")
    override fun getGitDir(): VirtualFile {
        throw UnsupportedOperationException()
    }

    override fun getRepositoryFiles(): GitRepositoryFiles {
        throw UnsupportedOperationException()
    }

    override fun getStagingAreaHolder(): GitStagingAreaHolder {
        throw UnsupportedOperationException()
    }

    override fun getUntrackedFilesHolder(): GitUntrackedFilesHolder {
        throw UnsupportedOperationException()
    }

    override fun getInfo(): GitRepoInfo {
        throw UnsupportedOperationException()
    }

    override fun getCurrentBranch(): GitLocalBranch? {
        throw UnsupportedOperationException()
    }

    override fun getBranches(): GitBranchesCollection {
        throw UnsupportedOperationException()
    }

    override fun getRemotes(): MutableCollection<GitRemote> {
        throw UnsupportedOperationException()
    }

    override fun getBranchTrackInfos(): MutableCollection<GitBranchTrackInfo> {
        throw UnsupportedOperationException()
    }

    override fun getBranchTrackInfo(localBranchName: String): GitBranchTrackInfo? {
        throw UnsupportedOperationException()
    }

    override fun isRebaseInProgress(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun isOnBranch(): Boolean {
        throw UnsupportedOperationException()
    }

    override fun getSubmodules(): MutableCollection<GitSubmoduleInfo> {
        throw UnsupportedOperationException()
    }

    override fun getIgnoredFilesHolder(): GitRepositoryIgnoredFilesHolder {
        throw UnsupportedOperationException()
    }
}
