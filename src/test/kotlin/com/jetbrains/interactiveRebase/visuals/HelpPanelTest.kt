package com.jetbrains.interactiveRebase.visuals

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import javax.swing.JButton

class HelpPanelTest : BasePlatformTestCase() {
    fun testGeneralStructure() {
        val panel = HelpPanel(project)

        assertThat(panel.getComponent(0)).isInstanceOf(JButton::class.java)
        val button = panel.getComponent(0) as JButton
        assertThat(button.toolTipText).isEqualTo("Show help contents")
        assertThat(button.getClientProperty("JButton.buttonType")).isEqualTo("help")
    }
}
