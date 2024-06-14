package com.jetbrains.interactiveRebase.visuals

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.mockito.ArgumentMatchers.any
import java.awt.Desktop

class HelpPanelTest : BasePlatformTestCase() {
    private lateinit var helpPanel: HelpPanel
    private lateinit var desktopMock: Desktop

    override fun setUp() {
        super.setUp()
//        val desktopMock: Desktop = mock(Desktop::class.java)
//        doNothing().`when`(desktopMock).browse(any(URI::class.java))
//
//        helpPanel = HelpPanel(desktopMock)
    }

    fun testActionPerformed() {
//        val desktopMock: Desktop = mock(Desktop::class.java)
//        doNothing().`when`(desktopMock).browse(any(URI::class.java))
//        val helpAction = HelpPanel.MyHelpAction(desktopMock)
//        val event = ActionEvent(HelpPanel(), 2, null)
//        helpAction.actionPerformed(event)
//        val captor = ArgumentCaptor.forClass(URI::class.java)
//        verify(desktopMock).browse(captor.capture())
//        assertThat(captor.value.toString())
//            .isEqualTo(
//                "https://gitlab.ewi.tudelft.nl/cse2000-software-project" +
//                    "/2023-2024/cluster-p/12c/interactive-rebase-jetbrains/-/blob/main/README.md",
//            )
    }

    private inline fun <reified T> anyCustom(): T = any(T::class.java)
}
