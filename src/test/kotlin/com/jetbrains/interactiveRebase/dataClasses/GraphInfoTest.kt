package com.jetbrains.interactiveRebase.dataClasses

import com.intellij.testFramework.fixtures.BasePlatformTestCase

class GraphInfoTest : BasePlatformTestCase() {
    private lateinit var graphInfo: GraphInfo

    override fun setUp() {
        super.setUp()
        graphInfo = GraphInfo(BranchInfo("main"))
    }

    fun testAddListener() {
        val listener =
            object : GraphInfo.Listener {
                override fun onBranchChange() {
                    println("Branch changed")
                }
            }
        graphInfo.addListener(listener)
        assertTrue(graphInfo.listeners.contains(listener))
        graphInfo.listeners[0].dispose()
    }

    fun testChangeAddedBranch() {
        val branch = BranchInfo("feature")
        graphInfo.changeAddedBranch(branch)
        assertEquals(branch, graphInfo.addedBranch)
    }

    fun testCreateBranchWithAddedBranch() {
        val branch = BranchInfo("feature")
        val branch2 = BranchInfo("feature2")
        val graphInfo = GraphInfo(branch, branch2)
        assertNotNull(graphInfo.addedBranch)
    }
}
