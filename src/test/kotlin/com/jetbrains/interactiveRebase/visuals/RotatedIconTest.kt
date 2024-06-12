package com.jetbrains.interactiveRebase.visuals

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.awt.Component
import java.awt.Graphics2D
import javax.swing.Icon

class RotatedIconTest : BasePlatformTestCase() {
    private lateinit var icon: Icon
    private lateinit var graphics2D: Graphics2D
    private lateinit var component: Component
    private val angle = 45.0

    override fun setUp() {
        super.setUp()
        icon = mock(Icon::class.java)
        graphics2D = mock(Graphics2D::class.java)
        component = mock(Component::class.java)
    }

    fun testPaintIcon() {
        val rotatedIcon = RotatedIcon(icon, angle)
        val x = 10
        val y = 20

        `when`(graphics2D.create()).thenReturn(graphics2D)
        `when`(icon.iconWidth).thenReturn(100)
        `when`(icon.iconHeight).thenReturn(50)

        rotatedIcon.paintIcon(component, graphics2D, x, y)

        verify(graphics2D).rotate(Math.toRadians(angle), (x + 100 / 2).toDouble(), (y + 50 / 2).toDouble())
        verify(icon).paintIcon(component, graphics2D, x, y)
        verify(graphics2D).transform = any()
    }
}
