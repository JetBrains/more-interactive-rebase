import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.geom.Ellipse2D
import kotlin.math.min


/**
 * Visual representation of commit node in the git graph
 */
class CirclePanel(private val diameter: Double, private val border: Float) : JBPanel<JBPanel<*>>() {

    // Flag to track whether the mouse is currently hovering over the circle
    private var isHovering = false
    private var isSelected = false
    private var centerX = 0.0
    private var centerY = 0.0
    private lateinit var circle: Ellipse2D

    init {

        isOpaque = false
        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                if (e != null && circle.contains(e.x.toDouble(), e.y.toDouble())) {
                    isHovering = true
                    repaint()
                }
            }

            override fun mouseExited(e: MouseEvent?) {
                if (e != null && !circle.contains(e.x.toDouble(), e.y.toDouble())) {
                    isHovering = false
                    repaint()
                }
            }

            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                isSelected = !isSelected
                repaint()
            }
        })

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent?) {
                isHovering = e != null && circle.contains(e.x.toDouble(), e.y.toDouble())
                repaint()
            }
        })
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        // Cast the Graphics object to Graphics2D for more advanced rendering
        val g2d = g as Graphics2D

        // Set rendering hints for smoother rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        createCircle()

        if (isSelected) {
            g2d.color = JBColor(Color(32, 112, 179), Color(21, 88, 155))
        } else {
            g2d.color = JBColor(Color(52, 152, 219), Color(41, 128, 185))
//            g2d.color = JBColor(Color(234, 217, 76), Color(194, 174, 23))
//            g2d.color = JBColor(Color(231, 76, 60), Color(192, 57, 43))
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
        // Calculate the diameter of the circle
        val diameter = diameter - border

        // Calculate the x and y coordinates for drawing the circle at the center
        centerX = (width - diameter) / 2
        centerY = (height - diameter) / 2
        circle = Ellipse2D.Double(centerX, centerY, diameter, diameter)
    }
}
