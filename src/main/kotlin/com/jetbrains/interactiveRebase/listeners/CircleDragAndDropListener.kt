import com.jetbrains.interactiveRebase.visuals.BranchPanel
import com.jetbrains.interactiveRebase.visuals.Palette
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import kotlin.math.abs

data class CirclePosition(
    val centerX: Int,
    val centerY: Int,
    val x: Int,
    val y: Int
)

class CircleDragAndDropListener(
    private val circle: CirclePanel,
    private val circles: MutableList<CirclePanel>,
    private val parent: BranchPanel
) : MouseAdapter() {
    private var initialX = circle.x
    private var initialY = circle.y
    private var currentX = 0
    private var currentY = 0
    private var initialIndex = 0
    private var circlesPositions = mutableListOf<CirclePosition>()

    override fun mousePressed(e: MouseEvent) {
        currentX = e.xOnScreen
        currentY = e.yOnScreen
        initialIndex = circles.indexOf(circle)
        circlesPositions = circles.map { c ->
            CirclePosition(c.centerX.toInt(), c.centerY.toInt(), c.x, c.y)
        }.toMutableList()
    }

    override fun mouseDragged(e: MouseEvent) {
        val deltaX = e.xOnScreen - currentX
        val deltaY = e.yOnScreen - currentY
        val newX = circle.x + deltaX
        val newY = circle.y + deltaY
        circle.color = Palette.TOMATO
        circle.setLocation(newX, newY)
        currentX = e.xOnScreen
        currentY = e.yOnScreen
    }

    override fun mouseReleased(e: MouseEvent) {
        val newIndex = findDropIndex(circle)
        if (newIndex != -1 && newIndex != initialIndex) {
            circles.removeAt(initialIndex)
            circles.add(newIndex, circle)
            repositionCircles(circles)
        } else {
            circle.setLocation(initialX, initialY)
        }
        parent.repaint()
    }

    private fun findDropIndex(circle: CirclePanel): Int {
        var newIndex = -1
        var closestDistance = Int.MAX_VALUE
        val newY = circle.y

        for ((index, pos) in circlesPositions.withIndex()) {
            val distance = abs(newY - pos.centerY)
            if (distance < closestDistance) {
                newIndex = index
                closestDistance = distance
            }
        }
        return newIndex
    }

    private fun repositionCircles(circles: MutableList<CirclePanel>) {
        for (i in circles.indices) {
            val circle = circles[i]
            circle.setLocation(circlesPositions[i].x, circlesPositions[i].y)
        }
    }
}
