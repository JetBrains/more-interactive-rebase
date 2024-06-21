package com.jetbrains.interactiveRebase.visuals

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

class RoundedButtonTest : BasePlatformTestCase() {
    private lateinit var roundedButton: RoundedButton
    private lateinit var g2d: Graphics2D

    fun testPaintComponent() {
        roundedButton = spy(RoundedButton())
        roundedButton.setSize(100, 50)
        val image =
            BufferedImage(
                roundedButton.width,
                roundedButton.height,
                BufferedImage.TYPE_INT_ARGB,
            )
        g2d = spy(image.createGraphics())

        roundedButton.paintComponent(g2d)

        verify(roundedButton).isContentAreaFilled = false
        verify(g2d).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(g2d).color = roundedButton.backgroundColor
        verify(g2d).fill(
            RoundRectangle2D.Double(
                1.0,
                3.0,
                roundedButton.width.toDouble() - 3.0,
                roundedButton.height.toDouble() - 6.0,
                roundedButton.arcWidth.toDouble(),
                roundedButton.arcHeight.toDouble(),
            ),
        )

        verify(g2d).dispose()
        verify(roundedButton).setText("")
        verify(roundedButton).setBackground(Palette.TRANSPARENT)
        verify(roundedButton).setForeground(Palette.TRANSPARENT)
    }

    fun testPaintComponentThreeFields() {
        roundedButton = spy(RoundedButton("blah", Palette.TOMATO, Palette.BLUE))
        roundedButton.setSize(100, 50)
        val image =
            BufferedImage(
                roundedButton.width,
                roundedButton.height,
                BufferedImage.TYPE_INT_ARGB,
            )
        g2d = spy(image.createGraphics())

        roundedButton.paintComponent(g2d)

        verify(roundedButton).isContentAreaFilled = false
        verify(g2d).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        verify(g2d).color = roundedButton.backgroundColor
        verify(g2d).fill(
            RoundRectangle2D.Double(
                1.0,
                3.0,
                roundedButton.width.toDouble() - 3.0,
                roundedButton.height.toDouble() - 6.0,
                roundedButton.arcWidth.toDouble(),
                roundedButton.arcHeight.toDouble(),
            ),
        )

        verify(g2d).dispose()
        verify(roundedButton).setText("blah")
        verify(roundedButton).setBackground(Palette.TOMATO)
        verify(roundedButton).setForeground(Palette.BLUE)
    }

    fun testPaintComponentTwoColors() {
        roundedButton = spy(RoundedButton("", Palette.TOMATO, Palette.BLUE))
        roundedButton.setSize(100, 50)
        val image =
            BufferedImage(
                roundedButton.width,
                roundedButton.height,
                BufferedImage.TYPE_INT_ARGB,
            )
        g2d = spy(image.createGraphics())

        roundedButton.paintComponent(g2d)

        verify(roundedButton).setText("")
        verify(roundedButton).setBackground(Palette.TOMATO)
        verify(roundedButton).setForeground(Palette.BLUE)
    }

    fun testPaintComponentTextColor() {
        roundedButton = spy(RoundedButton("blah", Palette.TOMATO))
        roundedButton.setSize(100, 50)
        val image =
            BufferedImage(
                roundedButton.width,
                roundedButton.height,
                BufferedImage.TYPE_INT_ARGB,
            )
        g2d = spy(image.createGraphics())

        roundedButton.paintComponent(g2d)

        verify(roundedButton).setText("blah")
        verify(roundedButton).setBackground(Palette.TOMATO)
        verify(roundedButton).setForeground(Palette.TRANSPARENT)
    }

    fun testPaintComponentTextColorTwo() {
        roundedButton = spy(RoundedButton("blah", foreground = Palette.TOMATO))
        roundedButton.setSize(100, 50)
        val image =
            BufferedImage(
                roundedButton.width,
                roundedButton.height,
                BufferedImage.TYPE_INT_ARGB,
            )
        g2d = spy(image.createGraphics())

        roundedButton.paintComponent(g2d)

        verify(roundedButton).setText("blah")
        verify(roundedButton).setBackground(Palette.TRANSPARENT)
        verify(roundedButton).setForeground(Palette.TOMATO)
    }
}
