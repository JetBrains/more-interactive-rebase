package com.jetbrains.interactiveRebase.visuals

import HeaderPanel
import com.intellij.ui.components.JBPanel
import org.junit.Test
import org.junit.jupiter.api.Assertions
import javax.swing.JButton

class HeaderPanelTest {
    @Test
    fun testAddGitButtons() {
        val mainPanel = JBPanel<JBPanel<*>>()
        val headerPanel = HeaderPanel(mainPanel)

        val gitActionsPanel = headerPanel.gitActionsPanel

        Assertions.assertEquals(4, gitActionsPanel.componentCount)
        Assertions.assertTrue(gitActionsPanel.getComponent(0) is JButton)
        Assertions.assertTrue(gitActionsPanel.getComponent(1) is JButton)
        Assertions.assertTrue(gitActionsPanel.getComponent(2) is JButton)
        Assertions.assertTrue(gitActionsPanel.getComponent(3) is JButton)
    }

    @Test
    fun testAddChangeButtons() {
        val mainPanel = JBPanel<JBPanel<*>>()
        val headerPanel = HeaderPanel(mainPanel)

        val changeActionsPanel = headerPanel.changeActionsPanel

        Assertions.assertEquals(2, changeActionsPanel.componentCount)
        Assertions.assertTrue(changeActionsPanel.getComponent(0) is JButton)
        Assertions.assertTrue(changeActionsPanel.getComponent(1) is JButton)
    }
}
