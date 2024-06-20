package com.jetbrains.interactiveRebase.integrationTests

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.testFramework.TestActionEvent.createTestEvent
import com.jetbrains.interactiveRebase.actions.buttonActions.StartRebaseAction
import com.jetbrains.interactiveRebase.actions.changePanel.CollapseAction
import com.jetbrains.interactiveRebase.actions.gitPanel.FixupAction
import com.jetbrains.interactiveRebase.actions.gitPanel.PickAction
import com.jetbrains.interactiveRebase.actions.gitPanel.StopToEditAction
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.git
import com.jetbrains.interactiveRebase.listeners.CircleHoverListener
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.ModelService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.awaitility.Awaitility
import java.awt.event.MouseEvent
import java.util.concurrent.TimeUnit

/**
 * Tests a use case in which the following actions are performed:
 * 1. Un-collapses all collapsed commits
 * 2. Stop to edit a commit ("sixth")
 * 3. Tries to collapse 2 commits which are not in range
 * 4. Collapses commits ("fifth", "sixth", "seventh", "eight")
 * 5. Fixup a commit ("i love testing") with its parent
 * 6. Fixes up a commit ("code quality", which was already the parent of the
 *          previous fixup) with its parent and another commit ("first")
 * 7. Picks a commit ("first") => this reverts the fixup action
 * 8. Performs the interactive rebase action, which stops to edit the commit
 */
class UseCase2Test : IRGitPlatformTest() {
    lateinit var fifthCommitOnDev: String
    lateinit var sixthCommitOnDev: String
    lateinit var seventhCommitOnDev: String
    lateinit var eightCommitOnDev: String

    override fun setUp() {
        super.setUp()
        fifthCommitOnDev = createAndCommitNewFile("file10.txt", "fifth")
        sixthCommitOnDev = createAndCommitNewFile("file11.txt", "sixth")
        seventhCommitOnDev = createAndCommitNewFile("file22.txt", "seventh")
        eightCommitOnDev = createAndCommitNewFile("file222.txt", "eight")

        Awaitility.await()
            .atMost(15000, TimeUnit.MILLISECONDS)
            .pollDelay(50, TimeUnit.MILLISECONDS)
            .until {
                gitCommitsCountEquals(8)
            }
    }
    fun testUseCase2() {
        runBlocking(Dispatchers.EDT) {
            // this opens the editor tab, and initializes everything
            openAndInitializePlugin(8)
            val modelService = project.service<ModelService>()


            //since the amount of commits > 7, there are collapsed commits, and thus we need to un-collapse the commit
            val commitToUncollapse = modelService.branchInfo.currentCommits[5]
            val circlePanelOfCommit = project.service<ActionService>().
                                        mainPanel.graphPanel.
                                        mainBranchPanel.branchPanel.
                                        circles[5]
            val listenerForUncollapse = circlePanelOfCommit.mouseListeners.filterIsInstance<CircleHoverListener>()[0]

            val event = MouseEvent(circlePanelOfCommit, MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, 1, false)
            listenerForUncollapse.mouseClicked(event)
            assertThat(commitToUncollapse.isCollapsed).isFalse()


            // this selects the second-to-last commit and sets it up to be edited
            val commitToEdit = modelService.branchInfo.currentCommits[2]
            assertThat(commitToEdit.commit.subject).isEqualTo("sixth")
            modelService.selectSingleCommit(commitToEdit, modelService.branchInfo)

            val editAction = StopToEditAction()
            val testEvent1 = createTestEvent(editAction)
            editAction.update(testEvent1)
            assertThat(testEvent1.presentation.isEnabled).isTrue()

            editAction.actionPerformed(testEvent1)


            //this will try twice to collapse commits
            modelService.selectSingleCommit(modelService.branchInfo.currentCommits[1], modelService.branchInfo)
            modelService.addToSelectedCommits(modelService.branchInfo.currentCommits[4], modelService.branchInfo)

            //the first time it's not working because commits are not in range
            val collapseAction = CollapseAction()
            val testEvent2 = createTestEvent(collapseAction)
            collapseAction.update(testEvent2)

            assertThat(testEvent2.presentation.isEnabled).isFalse()

            modelService.selectSingleCommit(modelService.branchInfo.currentCommits[1], modelService.branchInfo)
            modelService.addToSelectedCommits(modelService.branchInfo.currentCommits[2], modelService.branchInfo)
            modelService.addToSelectedCommits(modelService.branchInfo.currentCommits[3], modelService.branchInfo)
            modelService.addToSelectedCommits(modelService.branchInfo.currentCommits[4], modelService.branchInfo)

            val testEvent3 = createTestEvent(collapseAction)
            collapseAction.update(testEvent3)
            assertThat(testEvent3.presentation.isEnabled).isTrue()

            collapseAction.actionPerformed(testEvent3)
            assertThat(modelService.branchInfo.currentCommits.size).isEqualTo(5)

            //does an automatic fixup with the parent commit
            val fixupAction1 = FixupAction()
            val testEvent4 = createTestEvent(fixupAction1)

            modelService.selectSingleCommit(modelService.branchInfo.currentCommits[2], modelService.branchInfo)
            fixupAction1.update(testEvent4)
            assertThat(testEvent4.presentation.isEnabled).isTrue()

            fixupAction1.actionPerformed(testEvent4)
            assertThat(modelService.branchInfo.currentCommits.size).isEqualTo(4)

            val testEvent5 = createTestEvent(fixupAction1)
            modelService.selectSingleCommit(modelService.branchInfo.currentCommits[2], modelService.branchInfo)
            modelService.addToSelectedCommits(modelService.branchInfo.currentCommits[3], modelService.branchInfo)
            fixupAction1.update(testEvent5)
            assertThat(testEvent4.presentation.isEnabled).isTrue()
            fixupAction1.actionPerformed(testEvent4)
            assertThat(modelService.branchInfo.currentCommits.size).isEqualTo(3)

            val pickAction = PickAction()
            val testEvent6 = createTestEvent(pickAction)
            modelService.selectSingleCommit(modelService.branchInfo.currentCommits[2], modelService.branchInfo)
            pickAction.update(testEvent6)

            assertThat(testEvent6.presentation.isEnabled).isTrue()
            pickAction.actionPerformed(testEvent6)
            assertThat(modelService.branchInfo.currentCommits.size).isEqualTo(5)

            // this clicks the rebase button
            val rebaseAction = StartRebaseAction()
            val rebaseEvent = createTestEvent(rebaseAction)
            rebaseAction.update(rebaseEvent)
            assertThat(rebaseEvent.presentation.isEnabled).isTrue()

            rebaseAction.actionPerformed(rebaseEvent)

            Awaitility.await()
                .atMost(20000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .alias("status is rebase in process")
                .pollInSameThread()
                .until {
                    val statusOutput = repository.git("status")
                    statusOutput.contains("rebase in process")
                    statusOutput.contains("sixth")
                }

            // this continues the rebase
            repository.git("rebase --continue")

            // this checks that the rebase was continued and finished
            Awaitility.await()
                .atMost(20000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .alias("no longer stopped to edit")
                .pollInSameThread()
                .until {
                    val statusOutput = repository.git("status")
                    statusOutput.contains("rebase in process").not()
                }

            assertThat(gitCommitsCountEquals(8)).isTrue()
            val remainingCommitMessages = repository.getAllCommitMessages()
            assertThat(remainingCommitMessages.containsAll(listOf("first", "code quality", "i love testing", "my final commit", "fifth", "sixth", "seventh", "eight"))).isTrue()
        }
    }
}
