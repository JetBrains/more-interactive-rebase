package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.GraphInfo
import com.jetbrains.interactiveRebase.services.ModelService
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.LinearGradientPaint
import java.awt.RenderingHints
import java.awt.geom.CubicCurve2D
import javax.swing.SwingConstants

/**
 * Draws one or two branches next to another
 * from their diverging point onward
 */
class GraphPanel(
    val project: Project,
//    graphInfo: GraphInfo,
//    mainBranch: BranchInfo,
//    addedBranch: BranchInfo? = null,
    private val mainColor: JBColor = Palette.BLUE,
    private val addedColor: JBColor = Palette.LIME_GREEN,
) : JBPanel<JBPanel<*>>() {
    val mainBranch = project.service<ModelService>().graphInfo.mainBranch
    val addedBranch = project.service<ModelService>().graphInfo.addedBranch
    val mainBranchPanel: LabeledBranchPanel =
        createLabeledBranchPanel(mainBranch, SwingConstants.RIGHT)

    var addedBranchPanel: LabeledBranchPanel? = null

    init {
        if (addedBranch != null) {
            addedBranchPanel =
                createLabeledBranchPanel(addedBranch, SwingConstants.LEFT)
            // TODO: remove
//            addedBranchPanel!!.border = BorderFactory.createLineBorder(JBColor.CYAN)
//            mainBranchPanel.border = BorderFactory.createLineBorder(JBColor.YELLOW)
        }

        layout = GridBagLayout()

        addBranches()
    }

    /**
     * Creates a panel representing the branch
     * with all the corresponding commit names and branch name
     * with a specified orientation of the labels (left/right alignment)
     */
    private fun createLabeledBranchPanel(
        mainBranch: BranchInfo,
        alignment: Int,
    ) = LabeledBranchPanel(
        project,
        mainBranch,
        mainColor,
        alignment,
    )

    /**
     * Adds the two branches (as Labeled Branch Panels)
     * to the graph panel
     */
    private fun addBranches() {
        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.anchor = GridBagConstraints.CENTER
        gbc.fill = GridBagConstraints.BOTH

        add(mainBranchPanel, gbc)

        gbc.gridx = 1
        gbc.insets =
            Insets(
                0,
                mainBranchPanel.branchPanel.diameter,
                0,
                0,
            )

        if (addedBranchPanel != null) {
            add(addedBranchPanel!!, gbc)
        }
    }

    /**
     * Draws the line from the diverging commit
     * to the second branch
     */
    public override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.color = Palette.BLUE
        g2d.stroke = BasicStroke(2f)
        g2d.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON,
        )

        // Coordinates of the last circle of the main branch
        val (mainCircleCenterX, mainCircleCenterY) = centerCoordinatesOfLastMainCircle()

        // Coordinates of the last circle of the added branch
        if (addedBranchPanel != null) {
            val (addedCircleCenterX, addedCircleCenterY) = centerCoordinatesOfLastAddedCircle()

            gradientTransition(
                g2d,
                mainCircleCenterX,
                mainCircleCenterY,
                addedCircleCenterX,
                addedCircleCenterY,
            )

            val curve =
                CubicCurve2D.Float(
                    mainCircleCenterX.toFloat(),
                    mainCircleCenterY.toFloat(),
                    mainCircleCenterX.toFloat(),
                    mainCircleCenterY.toFloat() + mainBranchPanel.branchPanel.diameter * 3 / 2,
                    mainCircleCenterX.toFloat(),
                    mainCircleCenterY.toFloat() + mainBranchPanel.branchPanel.diameter * 3 / 2,
                    addedCircleCenterX.toFloat(),
                    addedCircleCenterY.toFloat(),
                )

            g2d.draw(curve)
        }
    }

    /**
     * Find the coordinates of the center
     * of the last circle of the primary (checked out) branch
     */
    fun centerCoordinatesOfLastAddedCircle(): Pair<Int, Int> {
        val addedLastCircle = addedBranchPanel!!.branchPanel.circles.last()
        val addedCircleCenterX =
            addedBranchPanel!!.x + addedBranchPanel!!.branchPanel.x + addedLastCircle.x + addedLastCircle.width / 2
        val addedCircleCenterY =
            addedBranchPanel!!.y + addedBranchPanel!!.branchPanel.y + addedLastCircle.y + addedLastCircle.height / 2
        return Pair(addedCircleCenterX, addedCircleCenterY)
    }

    /**
     * Find the coordinates of the center
     * of the last circle of the added (not checked out) branch
     */
    fun centerCoordinatesOfLastMainCircle(): Pair<Int, Int> {
        val mainLastCircle = mainBranchPanel.branchPanel.circles.last()
        val mainCircleCenterX =
            mainBranchPanel.x + // start of the labeled branch panel
                mainBranchPanel.branchPanel.x + // start of the internal branch panel
                mainLastCircle.x + // start of the circle
                mainLastCircle.width / 2 // center of the circle
        val mainCircleCenterY =
            mainBranchPanel.y + // start of the labeled branch panel
                mainBranchPanel.branchPanel.y + // start of the internal branch panel
                mainLastCircle.y + // start of the circle
                mainLastCircle.height / 2 // center of the circle
        return Pair(mainCircleCenterX, mainCircleCenterY)
    }

    /**
     * Adds a gradient to the line
     * to blend the colors between the two branches
     */
    internal fun gradientTransition(
        g2d: Graphics2D,
        startX: Int,
        startY: Int,
        endX: Int,
        endY: Int,
        fractions: FloatArray = floatArrayOf(0.0f, 0.8f),
        colors: Array<Color> = arrayOf(mainColor, addedColor),
    ) {
        g2d.paint =
            LinearGradientPaint(
                startX.toFloat(),
                startY.toFloat(),
                endX.toFloat(),
                endY.toFloat(),
                fractions,
                colors,
            )
    }
}
