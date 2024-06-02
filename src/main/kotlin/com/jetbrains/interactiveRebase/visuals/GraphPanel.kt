package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.LinearGradientPaint
import javax.swing.SwingConstants

class GraphPanel(
    val project: Project,
    mainBranch: BranchInfo,
    addedBranch: BranchInfo? = null,
    private val mainColor: JBColor = Palette.BLUE,
    private val addedColor: JBColor = Palette.LIME,
) : JBPanel<JBPanel<*>>() {
    val mainBranchPanel: LabeledBranchPanel =
        LabeledBranchPanel(
            project,
            mainBranch,
            mainColor,
            SwingConstants.RIGHT,
        )
    var addedBranchPanel: LabeledBranchPanel? = null

    init {
        if (addedBranch != null) {
            addedBranchPanel =
                LabeledBranchPanel(
                    project,
                    addedBranch,
                    addedColor,
                    SwingConstants.LEFT,
                )
        }

        layout = GridBagLayout()

        addBranches()
    }

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

        add(addedBranchPanel!!, gbc)
    }

    public override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D
        g2d.color = Palette.BLUE
        g2d.stroke = BasicStroke(2f)

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

            g2d.drawLine(
                mainCircleCenterX,
                mainCircleCenterY,
                addedCircleCenterX,
                addedCircleCenterY,
            )
        }
    }

    /**
     * Find the coordinates of the center
     * of the last circle of the primary (checked out) branch
     */
    private fun centerCoordinatesOfLastAddedCircle(): Pair<Int, Int> {
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
    private fun centerCoordinatesOfLastMainCircle(): Pair<Int, Int> {
        val mainLastCircle = mainBranchPanel.branchPanel.circles.last()
        val mainCircleCenterX =
            mainBranchPanel.x + mainBranchPanel.branchPanel.x + mainLastCircle.x + mainLastCircle.width / 2
        val mainCircleCenterY =
            mainBranchPanel.y + mainBranchPanel.branchPanel.y + mainLastCircle.y + mainLastCircle.height / 2
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
    ) {
        val fractions = floatArrayOf(0.0f, 0.8f)
        val colors = arrayOf<Color>(mainColor, addedColor)

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
