package com.jetbrains.interactiveRebase.service

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo

class ComponentServiceTest : BasePlatformTestCase() {
    lateinit var mainComponent: JBPanel<JBPanel<*>>
    lateinit var branchInfo: BranchInfo

    override fun setUp() {
        super.setUp()
        mainComponent = JBPanel<JBPanel<*>>()
        branchInfo = BranchInfo("main", mutableListOf())
    }

//    fun testUpdateMainPanel() {
//        val componentService = ComponentService(mainComponent, branchInfo)
//
//        componentService.updateMainPanel()
//
//        assertEquals(2, mainComponent.componentCount)
//        val x = mainComponent.getComponent(0)
//        assertTrue(mainComponent.getComponent(0) is HeaderPanel)
//        assertTrue(mainComponent.getComponent(1) is JBPanel<*>)
//    }
}
