package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.Disposable
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/**
 * Visual representation of commit node in the git graph
 */
open class CirclePanel(
    open val diameter: Double,
    private val border: Float,
    var color: JBColor,
    open var commit: CommitInfo,
    open var next: CirclePanel? = null,
    open var previous: CirclePanel? = null,
) : JBPanel<JBPanel<*>>(), Disposable {
    var centerX = 0.0
    var centerY = 0.0
    lateinit var circle: Ellipse2D.Double

    /**
     * Makes a panel where the circle will be drawn and
     * sets listeners.
     */

    init {
        isOpaque = false
        minimumSize =
            Dimension(
                diameter.toInt(),
                // Ensure there is always spacing between circles
                // when drawing a branch
                (diameter.toInt() * 2.0).toInt(),
            )
        preferredSize = minimumSize
        createCircle(diameter)
    }

    /**
     * Draws circles within the circle panel
     * - if hovered put an outline
     * - if selected make color darker
     */

    public override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        paintCircle(g)
    }

    open fun paintCircle(g: Graphics) {
        val g2d = g as Graphics2D

        // Set rendering hints for smoother rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        createCircle(diameter)
        color =
            if (commit.isDragged) {
                JBColor.BLUE as JBColor
            } else if (commit.isReordered) {
                Palette.INDIGO
            } else {
                color
            }
        val circleColor =
            if (commit.isSelected) {
                color.darker() as JBColor
            } else {
                color
            }
        val borderColor =
            if (commit.isSelected) {
                Palette.BLUEBORDER.darker()
            } else if (commit.isDragged || commit.isReordered) {
                color.darker()
            } else {
                Palette.DARKBLUE
            }
        selectedCommitAppearance(g2d, commit.isSelected, circleColor, borderColor)
//        if (commit.isDragged) {
//            cursor = grabHandCursor()
//        } else
        if (commit.isHovered) {
//            cursor = openHandCursor()
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            g2d.color = JBColor.BLACK
            g2d.stroke = BasicStroke(border)
            g2d.draw(circle)
        } else {
            cursor = Cursor.getDefaultCursor()
        }
    }

    fun openHandCursor(): Cursor {
        val toolkit = Toolkit.getDefaultToolkit()
        val file = this::class.java.getResource("/open-hand-cursor.png")
        val image = ImageIO.read(file)
        val resizedImage = resizeImage(image, 1024, 1024)
        val hotSpot = Point(16, 16)
        return toolkit.createCustomCursor(resizedImage, hotSpot, "OpenHandCursor")
    }

    fun grabHandCursor(): Cursor {
        val toolkit = Toolkit.getDefaultToolkit()
        val file = this::class.java.getResource("/grab-hand-cursor.png")
        val image = ImageIO.read(file)
        val resizedImage = resizeImage(image, 1024, 1024)
        val hotSpot = Point(16, 16)
        return toolkit.createCustomCursor(resizedImage, hotSpot, "GrabHandCursor")
    }

    fun resizeImage(
        originalImage: BufferedImage,
        width: Int,
        height: Int,
    ): BufferedImage {
        val resizedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = resizedImage.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.drawImage(originalImage, 0, 0, width, height, null)
        g.dispose()
        return resizedImage
    }

    /**
     * Creates a circle shape to be drawn inside the panel.
     */
    open fun createCircle(diameter: Double) {
        val width = width.toDouble()
        val height = height.toDouble()

        // Calculate the diameter of the circle,
        // so that border is not cropped due to the panel size
        val adjustedDiameter = diameter - 2 * (border + 0.5)

        // Calculate the x and y coordinates for drawing the circle at the center
        val originX = (width - adjustedDiameter) / 2
        val originY = (height - adjustedDiameter) / 2

        centerX = this.x + adjustedDiameter / 2
        centerY = this.y + adjustedDiameter / 2
        circle = Ellipse2D.Double(originX, originY, adjustedDiameter, adjustedDiameter)
    }

    /**
     * Draws the circle with a shadow and border.
     */
    open fun selectedCommitAppearance(
        g2d: Graphics2D,
        isSelected: Boolean,
        circleColor: Color,
        borderColor: Color,
    ) {
        g2d.fill(circle)
        g2d.color = if (isSelected) circleColor.darker() else circleColor
        drawBorder(g2d, circle, borderColor)
    }

    /**
     * Draws the border of the circle.
     */
    open fun drawBorder(
        g2d: Graphics2D,
        circle: Ellipse2D.Double,
        borderColor: Color,
    ) {
        g2d.fill(circle)
        g2d.color = borderColor
        g2d.stroke = BasicStroke(border)
        g2d.draw(circle)
    }

    override fun dispose() {
    }
}
