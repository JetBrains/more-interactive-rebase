import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.listeners.CircleHoverListener
import java.awt.*
import java.awt.geom.Ellipse2D


/**
 * Visual representation of commit node in the git graph
 */
class CirclePanel(private val diameter: Double,
                  private val border: Float,
                  private val color: JBColor) : JBPanel<JBPanel<*>>() {

    // Flag to track whether the mouse is currently hovering over the circle
    var isHovering = false
    var isSelected = false
    private var centerX = 0.0
    private var centerY = 0.0
    lateinit var circle: Ellipse2D

    init {
        isOpaque = false
        preferredSize = minimumSize
        createCircle()
        addMouseListener(CircleHoverListener(this))
        addMouseMotionListener(CircleHoverListener(this))
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        // Cast the Graphics object to Graphics2D for more advanced rendering
        val g2d = g as Graphics2D

        // Set rendering hints for smoother rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        createCircle()

        if (isSelected) {
            g2d.color = color.darker()
        } else {
            g2d.color = color
        }
        g2d.fill(circle)

        if (isHovering) {
            g2d.color = JBColor.BLACK
            g2d.stroke = BasicStroke(border)
            g2d.draw(circle)
        }
    }

    /**
     * Creates a circle shape
     */
    private fun createCircle() {
        val width = width.toDouble()
        val height = height.toDouble()

        // Calculate the diameter of the circle,
        // so that border is not cropped due to the panel size
        val diameter = diameter - 2 * border

        // Calculate the x and y coordinates for drawing the circle at the center
        centerX = (width - diameter) / 2
        centerY = (height - diameter) / 2
        circle = Ellipse2D.Double(centerX, centerY, diameter, diameter)
    }
}
