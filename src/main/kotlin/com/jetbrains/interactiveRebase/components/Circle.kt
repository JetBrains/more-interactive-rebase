import com.intellij.ui.JBColor
import com.intellij.ui.tabs.impl.JBDefaultTabsBorder
import javax.swing.*
import java.awt.*
import java.awt.event.*
import java.awt.geom.Ellipse2D

class Circle : JComponent() {

    // Flag to track whether the mouse is currently hovering over the circle
    private var isHovering = false

    lateinit var circle : Ellipse2D

    init {


        // Add mouse listener to track mouse enter and exit events
        addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                if( e!= null && circle.contains(e.x.toDouble(),e.y.toDouble()))
                    isHovering = true
                repaint() // Repaint the component to update the appearance
            }

            override fun mouseExited(e: MouseEvent?) {
                //super.mouseExited(e)
                if(e!=null && !circle.contains(e.x.toDouble(),e.y.toDouble()))
                isHovering = false
                repaint() // Repaint the component to update the appearance
            }
        })

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseMoved(e: MouseEvent?) {
                if (e != null && circle.contains(e.x.toDouble(), e.y.toDouble())) {
                    isHovering = true
                    repaint() // Repaint the component to update the appearance
                } else {
                    isHovering = false
                    repaint() // Repaint the component to update the appearance
                }
            }
        })
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        // Cast the Graphics object to Graphics2D for more advanced rendering
        val g2d = g as Graphics2D

        // Set rendering hints for smoother rendering
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)


        val width = getWidth().toDouble()
        val height = getHeight().toDouble()
        val border = 1.5f

        // Calculate the diameter of the circle
        val diameter = Math.min(width, height) - border

        // Calculate the x and y coordinates for drawing the circle at the center
        val x = (width - diameter) / 2
        val y = (height - diameter) / 2
        circle = Ellipse2D.Double(x, y, diameter, diameter)

        g2d.color = JBColor.PINK
        g2d.fill(circle)
        // Set the border of the circle based on hover state
        if (isHovering) {
            g2d.color = JBColor.BLACK
            g2d.stroke = BasicStroke(border)
            g2d.draw(circle)
        }
    }
}
