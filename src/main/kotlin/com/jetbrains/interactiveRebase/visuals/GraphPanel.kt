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
import javax.swing.BorderFactory
import javax.swing.SwingConstants

/**
 * Draws one or two branches next to another
 * from their diverging point onward
 */
class GraphPanel(
    val project: Project,
    val graphInfo: GraphInfo = project.service<ModelService>().graphInfo,
    private val mainTheme: Palette.Theme = Palette.BLUE_THEME,
    private val addedTheme: Palette.Theme = Palette.TOMATO_THEME,
) : JBPanel<JBPanel<*>>() {
    var mainBranchPanel: LabeledBranchPanel =
        createLabeledBranchPanel(
            graphInfo.mainBranch,
            SwingConstants.RIGHT,
            mainTheme,
        )

    var addedBranchPanel: LabeledBranchPanel? = null

    init {
        if (graphInfo.addedBranch != null) {
            addedBranchPanel =
                createLabeledBranchPanel(
                    graphInfo.addedBranch!!,
                    SwingConstants.LEFT,
                    addedTheme,
                )
            addedBranchPanel!!.border = BorderFactory.createLineBorder(JBColor.GREEN)
        }
        mainBranchPanel.border = BorderFactory.createLineBorder(JBColor.BLUE)

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
        colorTheme: Palette.Theme,
    ) = LabeledBranchPanel(
        project,
        mainBranch,
        colorTheme,
        alignment,
    )

    /**
     * Adds the two branches (as Labeled Branch Panels)
     * to the graph panel
     */
    internal fun addBranches() {
        val gbc = GridBagConstraints()

        val (offsetMain, offsetAdded) = computeVerticalOffsets()
        addFirstBranchToTheView(gbc, offsetMain)

        if (addedBranchPanel != null) {
            addSecondBranchToTheView(gbc, offsetAdded)
        }
    }

    /**
     * Adds the second branch to the view
     * for visualization
     */
    private fun GraphPanel.addSecondBranchToTheView(
        gbc: GridBagConstraints,
        offset: Int,
    ) {
        alignSecondBranch(gbc)
        addedBranchPanel!!.addBranchWithVerticalOffset(offset)
        add(addedBranchPanel!!, gbc)
    }

    /**
     * Computes what offset is necessary for both of the branches
     * to be properly aligned.
     * That is, the added branch always has
     * a lower position than the primary branch.
     * The default padding is 5 px.
     */
    private fun computeVerticalOffsets(): Pair<Int, Int> {
        val offset = computeVerticalOffsetOfSecondBranch()
        var offsetMain = 5
        var offsetAdded = 5
        if (offset < 0) {
            offsetMain = -offset
        } else if (offset > 0) {
            offsetAdded = offset
        }
        return Pair(offsetMain, offsetAdded)
    }

    /**
     * Calculates a vertical offset for the second branch
     * based on the number of visualized commits
     * This ensures that the second added branch is always positioned
     * below the primary one
     */
    private fun computeVerticalOffsetOfSecondBranch(): Int {
        val mainCircleCount = mainBranchPanel.branchPanel.circles.size
        val addedCircleCount = addedBranchPanel?.branchPanel?.circles?.size ?: 0
        val difference = mainCircleCount - addedCircleCount + 2
        return difference * mainBranchPanel.branchPanel.diameter * 2
    }

    /**
     * Adds the main branch to the view
     * for visualization
     */
    private fun GraphPanel.addFirstBranchToTheView(
        gbc: GridBagConstraints,
        offset: Int,
    ) {
        alignPrimaryBranch(gbc)
        mainBranchPanel.addBranchWithVerticalOffset(offset)
        add(mainBranchPanel, gbc)
    }

    /**
     * Sets the grid alignment specifics
     * of the second branch
     * 1. it is to the right
     * 2. sets distance from the primary branch
     * 3. spans vertically over the entire editor tab
     */
    private fun alignSecondBranch(gbc: GridBagConstraints) {
        gbc.gridx = 1
        gbc.gridy = 0
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.anchor = GridBagConstraints.NORTH
        gbc.insets =
            Insets(
                0,
                mainBranchPanel.branchPanel.diameter,
                0,
                0,
            )
        gbc.fill = GridBagConstraints.BOTH
    }

    /**
     * Sets the grid alignment specifics
     * of the first branch
     * If it's the only branch set it to span vertically
     * else make sure the branch panel doesn't go all the way to the
     * bottom of the screen
     */
    private fun alignPrimaryBranch(gbc: GridBagConstraints) {
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.0
        gbc.weighty = 1.0
        gbc.anchor = GridBagConstraints.NORTH
        gbc.fill = GridBagConstraints.BOTH

        if (graphInfo.mainBranch.isPrimary) {
            gbc.fill = GridBagConstraints.HORIZONTAL
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
                    mainCircleCenterY.toFloat() + mainBranchPanel.branchPanel.diameter * 2,
                    mainCircleCenterX.toFloat(),
                    mainCircleCenterY.toFloat() + mainBranchPanel.branchPanel.diameter * 2,
                    addedCircleCenterX.toFloat(),
                    addedCircleCenterY.toFloat(),
                )

            // If added branch is not rendered because the screen is too small
            // coordinates appear to be 0
            // Hence, we don't draw the line in this case
            if (Pair(addedCircleCenterX, addedCircleCenterY) != Pair(0, 0)) {
                g2d.draw(curve)
            }
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
        var mainCircleCenterX = 0
        var mainCircleCenterY = 0
        if (mainBranchPanel.branchPanel.circles.isNotEmpty()) {
            val mainLastCircle = mainBranchPanel.branchPanel.circles.last()
            mainCircleCenterX =
                mainBranchPanel.x + // start of the labeled branch panel
                mainBranchPanel.branchPanel.x + // start of the internal branch panel
                mainLastCircle.x + // start of the circle
                mainLastCircle.width / 2 // center of the circle
            mainCircleCenterY =
                mainBranchPanel.y + // start of the labeled branch panel
                mainBranchPanel.branchPanel.y + // start of the internal branch panel
                mainLastCircle.y + // start of the circle
                mainLastCircle.height / 2 // center of the circle
        }
        return Pair(mainCircleCenterX, mainCircleCenterY)
    }

    /**
     * Update branch panels
     */
    fun updateGraphPanel() {
        removeAll()

        mainBranchPanel =
            createLabeledBranchPanel(
                graphInfo.mainBranch,
                SwingConstants.RIGHT,
                mainTheme,
            )

        addedBranchPanel = null
        if (graphInfo.addedBranch != null) {
            addedBranchPanel =
                createLabeledBranchPanel(
                    graphInfo.addedBranch!!,
                    SwingConstants.LEFT,
                    addedTheme,
                )
        }

        addBranches()
        revalidate()
        repaint()
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
        colors: Array<Color> = arrayOf(mainTheme.regularCircleColor, addedTheme.regularCircleColor),
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
