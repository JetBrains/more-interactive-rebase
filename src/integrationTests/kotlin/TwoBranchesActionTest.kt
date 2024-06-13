package com.jetbrains.interactiveRebase.integrationTests

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.testFramework.TestActionEvent.createTestEvent
import com.jetbrains.interactiveRebase.actions.CreateEditorTabAction
import com.jetbrains.interactiveRebase.actions.changePanel.AddBranchAction
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.checkout
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.checkoutNew
import com.jetbrains.interactiveRebase.integrationTests.git4ideaTestClasses.git
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.visuals.multipleBranches.SidePanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.awaitility.Awaitility
import java.awt.event.MouseEvent
import java.util.concurrent.TimeUnit

class TwoBranchesActionTest:IRGitPlatformTest(){
    lateinit var secondCommitOnMain: String
    lateinit var thirdCommitOnMain: String

    var featureBranch: String = "feature"
    lateinit var firstCommitOnFeature: String
    lateinit var secondCommitOnFeature: String
    lateinit var thirdCommitOnFeature: String
    lateinit var fourthCommitOnFeature: String
    lateinit var fifthCommitOnFeature: String


    override fun setUp() {
        super.setUp()
        runBlocking(Dispatchers.IO) {
            repository.checkout("main")
            assertCorrectCheckedOutBranch("main")

            secondCommitOnMain = createAndCommitNewFile("file5.txt", "second")
            thirdCommitOnMain = createAndCommitNewFile("file6.txt", "third")

            Awaitility.await()
                .atMost(15000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until {
                    gitCommitsCountEquals(2)
                }

            repository.checkoutNew(featureBranch)
            assertCorrectCheckedOutBranch(featureBranch)

            repository.git("branch")

            firstCommitOnFeature = createAndCommitNewFile("file7.txt", "refactor")
            secondCommitOnFeature = createAndCommitNewFile("file8.txt", "whatever")
            thirdCommitOnFeature = createAndCommitNewFile("file9.txt", "it works")
            fourthCommitOnFeature = createAndCommitNewFile("file10.txt", "testy")
            fifthCommitOnFeature = createAndCommitNewFile("file11.txt", "new file")

            assertCorrectCheckedOutBranch(featureBranch)
            Awaitility.await()
                .atMost(15000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until {
                    gitCommitsCountEquals(7)
                }
        }
    }

    override fun openAndInitializePlugin() {
        assertCorrectCheckedOutBranch(featureBranch)
        val openEditorTabAction = CreateEditorTabAction()
        val testEvent = createTestEvent(openEditorTabAction)
        assertThat(testEvent.project).isEqualTo(project)

        openEditorTabAction.actionPerformed(testEvent)

        val modelService = project.service<ModelService>()
        Awaitility.await()
            .atMost(15000, TimeUnit.MILLISECONDS)
            .pollDelay(50, TimeUnit.MILLISECONDS)
            .until { modelService.branchInfo.initialCommits.size == 5 }
        assertThat(modelService.branchInfo.name).isEqualTo(featureBranch)
    }

    fun testTwoBranchesInView(){
        runBlocking(Dispatchers.EDT){
            openAndInitializePlugin()
            val modelService = project.service<ModelService>()

            //open the side panel
            val addBranchAction = AddBranchAction()
            val testEvent1 = createTestEvent(addBranchAction)
            addBranchAction.actionPerformed(testEvent1)

            val sidePanel = project.service<ActionService>().
                                mainPanel.sidePanel.
                                    viewport.getComponent(0) as SidePanel
            assertThat(sidePanel.isVisible).isTrue()

            Awaitility.await()
                .pollInSameThread()
                .alias("open side panel and check if correct branches are displayed")
                .atMost(30000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until {
                    sidePanel.branches.equals(listOf("development","main"))
                }

            val mainBranchPanel = sidePanel.sideBranchPanels[1]
            assertThat(mainBranchPanel.branchName).isEqualTo("main")

            //to select a branch to add to the view
            val mainBranchPanelListener = mainBranchPanel.mouseListeners[0]
            val mouseEvent = MouseEvent(mainBranchPanel, 444, 0L, 0,2,2,1,false)

            mainBranchPanelListener.mouseClicked(mouseEvent)

            Awaitility.await()
                .atMost(15000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .alias("adding a second branch to the view and refreshing")
                .pollInSameThread()
                .until {
                   modelService.graphInfo.addedBranch != null
                }

            //check if the correct information about the 2 branches is added to the model
            val addedBranch = modelService.graphInfo.addedBranch
            Awaitility.await()
                .pollInSameThread()
                .alias("add main to view")
                .atMost(15000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until {
                    addedBranch?.name == "main"
                }

            Awaitility.await()
                .pollInSameThread()
                .alias("correct commits on main branch")
                .atMost(15000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .until {
                    val commitsOnMainBranch = addedBranch?.initialCommits?.map { it.commit.subject }
                    commitsOnMainBranch == listOf("third")
                }

            assertThat(modelService.graphInfo.mainBranch.name).isEqualTo(featureBranch)
            assertThat(modelService.graphInfo.mainBranch.initialCommits.map { it.commit.subject }).
                    isEqualTo(listOf("new file","testy","it works","whatever","refactor"))

            mainBranchPanelListener.mouseClicked(mouseEvent)

            Awaitility.await()
                .atMost(15000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .alias("removing a second branch from the view and refreshing")
                .pollInSameThread()
                .until {
                    modelService.graphInfo.addedBranch == null
                }

            val devBranchPanel = sidePanel.sideBranchPanels[0]
            assertThat(devBranchPanel.branchName).isEqualTo("development")

            //to select a branch to add to the view
            val devBranchPanelListener = devBranchPanel.mouseListeners[0]
            val mouseEvent2 = MouseEvent(devBranchPanel, 9, 0L, 0,2,2,1,false)

            devBranchPanelListener.mouseClicked(mouseEvent2)

            Awaitility.await()
                .atMost(15000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .alias("adding dev branch to the view and refreshing")
                .pollInSameThread()
                .until {
                    modelService.graphInfo.addedBranch != null
                }

            //check if the correct information about the 2 branches is added to the model
            val devBranch = modelService.graphInfo.addedBranch

            assertThat(devBranch?.name).isEqualTo("development")

            Awaitility.await()
                .atMost(15000, TimeUnit.MILLISECONDS)
                .pollDelay(50, TimeUnit.MILLISECONDS)
                .alias("check commits in dev branch are correct")
                .pollInSameThread()
                .until {
                    val commitsOnDevBranch = devBranch?.initialCommits?.map { it.commit.subject }
                    commitsOnDevBranch == listOf("my final commit", "i love testing", "code quality","first","initial")
                }

            assertThat(modelService.graphInfo.mainBranch.name).isEqualTo(featureBranch)
            assertThat(modelService.graphInfo.mainBranch.initialCommits.map { it.commit.subject }).
            isEqualTo(listOf("new file","testy","it works","whatever","refactor","third", "second"))
        }

    }
}