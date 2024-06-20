package com.jetbrains.interactiveRebase.integrationTests

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.vcs.Executor.overwrite
import com.intellij.openapi.vcs.Executor.touch
import com.intellij.testFramework.TestActionEvent.createTestEvent
import com.intellij.testFramework.common.runAll
import com.intellij.testFramework.replaceService
import com.intellij.vcs.test.VcsPlatformTest
import com.jetbrains.interactiveRebase.actions.CreateEditorTabAction
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.IRTestGitImpl
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.addCommit
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.assumeSupportedGitVersion
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.checkoutNew
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.createRepository
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.git
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.gitExecutable
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.visuals.RoundedButton
import git4idea.GitVcs
import git4idea.commands.Git
import git4idea.config.GitExecutableManager
import git4idea.config.GitVcsApplicationSettings
import git4idea.config.GitVcsSettings
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.awaitility.Awaitility
import org.junit.jupiter.api.TestInstance
import java.util.concurrent.TimeUnit

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
abstract class IRGitPlatformTest : VcsPlatformTest() {
    lateinit var git: IRTestGitImpl
    lateinit var repository: GitRepository
    lateinit var repositoryManager: GitRepositoryManager
    lateinit var vcs: GitVcs
    lateinit var settings: GitVcsSettings
    lateinit var appSettings: GitVcsApplicationSettings
    var developmentBranch: String = "development"

    lateinit var initialCommitOnMain: String

    lateinit var firstCommitOnDev: String
    lateinit var secondCommitOnDev: String
    lateinit var thirdCommitOnDev: String
    lateinit var fourthCommitOnDev: String

    lateinit var checkedOutBranch: String

    /**
     * Setup method that:
     *
     * - creates a repository
     * - makes the branch "main" the default one
     * - makes an initial commit
     * - checks out the "development" branch
     * - creates a file and commits it
     */
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        git = IRTestGitImpl()
        ApplicationManager.getApplication().replaceService(Git::class.java, git, testRootDisposable)
        repositoryManager = GitRepositoryManager.getInstance(project)

        vcs = GitVcs.getInstance(project)
        vcs.doActivate()

        settings = GitVcsSettings.getInstance(project)
        appSettings = GitVcsApplicationSettings.getInstance()
        appSettings.setPathToGit(gitExecutable())
        GitExecutableManager.getInstance().testGitExecutableVersionValid(project)
        assumeSupportedGitVersion(vcs)
        runBlocking(Dispatchers.IO) {
            // This creates a new repository in the test project root
            repository = createRepository(project, projectNioRoot, true)
            git("config --global init.defaultBranch main")
            initialCommitOnMain = git("rev-list --max-parents=0 HEAD")

            // This creates a new branch called "development",
            // and leaves the state to the new branch (checked out on branch "development")
            repository.checkoutNew(developmentBranch)
            assertCorrectCheckedOutBranch(developmentBranch)

            firstCommitOnDev = createAndCommitNewFile("file1.txt", "first")

            secondCommitOnDev = createAndCommitNewFile("file2.txt", "code quality")

            thirdCommitOnDev = createAndCommitNewFile("file3.txt", "i love testing")

            fourthCommitOnDev = createAndCommitNewFile("file4.txt", "my final commit")

            Awaitility.await()
                .alias("setup has 4 commits")
                .pollInSameThread()
                .atMost(10000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until { gitCommitsCountEquals(4) }
        }
    }

    override fun tearDown() {
        runAll(
            { if (::git.isInitialized) git.reset() },
            { if (::settings.isInitialized) settings.appSettings.setPathToGit(null) },
            { super.tearDown() },
        )
    }

    /**
     * Debugging method that allows to see all commits in the checked out branch.
     */
    fun GitRepository.getAllCommitMessages(): List<String> =
        git(project, "log --pretty=format:%s")
            .lines()
            .filter { it.isNotBlank() }

    fun GitRepository.getCheckedOutBranch(): String {
        return git(project, "rev-parse --abbrev-ref HEAD")
    }

    fun createAndCommitNewFile(
        fileName: String,
        commitMessage: String,
    ): String {
        touch(fileName)
        git(project, "add $fileName")
        return addCommit(commitMessage)
    }

    private fun modifyAndCommitFile(
        fileName: String,
        commitMessage: String,
    ): String {
        overwrite(fileName, "content" + Math.random())
        return addCommit(commitMessage)
    }

    fun countCommitsSinceInitialCommit(): Int {
        val result = repository.git("rev-list --count " + initialCommitOnMain + "..HEAD")
        Thread.sleep(10)
        return result.toInt()
    }

    fun gitCommitsCountEquals(expectedCount: Int): Boolean {
        return countCommitsSinceInitialCommit() == expectedCount
    }

    open fun openAndInitializePlugin(expectedCount: Int = 4) {
        assertCorrectCheckedOutBranch(developmentBranch)
        val openEditorTabAction = CreateEditorTabAction()
        val testEvent = createTestEvent(openEditorTabAction)

        assertThat(testEvent.project).isEqualTo(project)

        openEditorTabAction.actionPerformed(testEvent)

        val modelService = project.service<ModelService>()
        Awaitility.await()
            .atMost(15000, TimeUnit.MILLISECONDS)
            .pollDelay(50, TimeUnit.MILLISECONDS)
            .until { modelService.branchInfo.initialCommits.size == expectedCount }
        assertThat(modelService.branchInfo.name).isEqualTo(developmentBranch)
    }

    internal fun assertCorrectCheckedOutBranch(branchName: String) {
        checkedOutBranch = repository.getCheckedOutBranch()
        assertThat(checkedOutBranch).isEqualTo(branchName)
    }

    internal fun getRebaseButton(): RoundedButton {
        val headerPanel = project.service<ActionService>().getHeaderPanel()
        val changesActionsPanel = headerPanel.changeActionsPanel
        return changesActionsPanel.components[2] as RoundedButton
    }

    fun countCommitsSinceSpecificCommit(hash: String): Int {
        val result = repository.git("rev-list --count " + hash + "..HEAD")
        Thread.sleep(10)
        return result.toInt()
    }


}
