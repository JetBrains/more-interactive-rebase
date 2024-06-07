package com.jetbrains.interactiveRebase.integrationTests

import com.intellij.vcs.test.VcsPlatformTest
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.TestFile
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.addCommit
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.checkoutNew
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.createRepository
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.git
import git4idea.repo.GitRepository
import java.io.File

abstract class IRGitPlatformTest : VcsPlatformTest() {
    init {
        System.setProperty("idea.home.path", "/tmp")
    }

    lateinit var repository: GitRepository
    var developmentBranch: String = "development"

    lateinit var initialCommit: String
    lateinit var file1: TestFile
    lateinit var commit1: String

    /**
     * Setup method that:
     *
     * - creates a repository
     * - makes the branch "main" the default one
     * - makes an initial commit
     * - checks out the "development" branch
     * - creates a file and commits it
     */
    override fun setUp() {
        super.setUp()
        // This creates a new repository in the test project root
        repository = createRepository(project, projectNioRoot, true)

        // This creates a new branch called "development",
        // and leaves the state to the new branch (checked out on branch "development")
        repository.checkoutNew(developmentBranch)

        // This creates a new file in the repository and commits it to "development" branch
        file1 = TestFile(repository, File(projectRoot.path, "file1.txt"))
        file1.create("initial content")
        commit1 = addCommit("Changed file")

        initialCommit = git("rev-list --max-parents=0 HEAD")
    }

    /**
     * Debugging method that allows to see all commits in the checked out branch.
     */
    fun GitRepository.getAllCommits(): List<String> =
        git(project, "log --pretty=format:%H")
            .lines()
            .filter { it.isNotBlank() }
}
