package com.jetbrains.interactiveRebase.visuals

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.assertj.core.api.AssertionsForClassTypes.assertThat

class TextStyleTest : BasePlatformTestCase() {
    fun testStripLabelStrips() {
        val res = TextStyle.stripTextFromStyling("<html><strike><i>text here</i></strike></html>")
        assertThat(res).isEqualTo("text here")
    }

    fun testStripLabelStripsNormalString() {
        val res = TextStyle.stripTextFromStyling("text here")
        assertThat(res).isEqualTo("text here")
    }

    fun testGetStyleChecks() {
        val ital = TextStyle.getStyleTag(TextStyle.ITALIC)
        assertThat(ital).isEqualTo(Pair("<i>", "</i>"))
        val bold = TextStyle.getStyleTag(TextStyle.BOLD)
        assertThat(bold).isEqualTo(Pair("<b>", "</b>"))
        val cross = TextStyle.getStyleTag(TextStyle.CROSSED)
        assertThat(cross).isEqualTo(Pair("<strike>", "</strike>"))
    }

    fun testAddStylingAddsToNotStyled() {
        assertThat(TextStyle.addStyling("add ", TextStyle.BOLD)).isEqualTo("<html><b>add </b></html>")
    }

    fun testAddStylingToStyled() {
        assertThat(
            TextStyle.addStyling("<html><b>add style</b></html>", TextStyle.ITALIC),
        ).isEqualTo("<html><i><b>add style</b></i></html>")
    }
}
