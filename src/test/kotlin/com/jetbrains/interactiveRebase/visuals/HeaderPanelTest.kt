package com.jetbrains.interactiveRebase.visuals

//import com.intellij.openapi.components.service
//import com.intellij.testFramework.fixtures.BasePlatformTestCase
//import com.intellij.ui.components.JBPanel
//import com.jetbrains.interactiveRebase.services.RebaseInvoker
//import org.junit.Test
//import org.junit.jupiter.api.Assertions
//import javax.swing.JButton

//class HeaderPanelTest : BasePlatformTestCase() {
//    @Test
//    fun testAddGitButtons() {
//        val mainPanel = JBPanel<JBPanel<*>>()
//        val headerPanel = HeaderPanel(mainPanel, project, project.service<RebaseInvoker>())
//
//        val gitActionsPanel = headerPanel.gitActionsPanel
//
//        Assertions.assertEquals(4, gitActionsPanel.componentCount)
//        Assertions.assertTrue(gitActionsPanel.getComponent(0) is JButton)
//        Assertions.assertTrue(gitActionsPanel.getComponent(1) is JButton)
//        Assertions.assertTrue(gitActionsPanel.getComponent(2) is JButton)
//        Assertions.assertTrue(gitActionsPanel.getComponent(3) is JButton)
//    }
//
//    @Test
//    fun testAddChangeButtons() {
//        val mainPanel = JBPanel<JBPanel<*>>()
//        val headerPanel = HeaderPanel(mainPanel, project, project.service<RebaseInvoker>())
//
//        val changeActionsPanel = headerPanel.changeActionsPanel
//
//        Assertions.assertEquals(2, changeActionsPanel.componentCount)
//        Assertions.assertTrue(changeActionsPanel.getComponent(0) is JButton)
//        Assertions.assertTrue(changeActionsPanel.getComponent(1) is JButton)
//    }
//}
