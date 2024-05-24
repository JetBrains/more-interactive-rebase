package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.Disposable
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import java.awt.*
import java.awt.geom.Ellipse2D
import javax.swing.ImageIcon

/**
 * Visual representation of commit node in the git graph
 */
open class CirclePanel(
    val diameter: Double,
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
        minimumSize = Dimension(diameter.toInt(), diameter.toInt())
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

    public open fun paintCircle(g: Graphics) {
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

        if (commit.isHovered) {
//            setCursor(grabCursor())
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            g2d.color = JBColor.BLACK
            g2d.stroke = BasicStroke(border)
            g2d.draw(circle)
        } else {
            setCursor(Cursor.getDefaultCursor())
        }
    }

//    fun grabCursor(): Cursor {
//        val grabImagePath = "Images/commit.png"
//        val grabImage: Image = ImageIcon(grabImagePath).image
//
//        val toolkit = Toolkit.getDefaultToolkit()
//        return toolkit.createCustomCursor(grabImage, Point(0, 0), "Grab")
//
//    }

    /**
     * Creates a circle shape to be drawn inside the panel.
     */
    fun createCircle(diameter: Double) {
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

    /**
     * Draws the circle with a shadow and border.
     */
    fun paintSuper(g: Graphics) {
        super.paintComponent(g)
    }

    override fun dispose() {
    }
}
