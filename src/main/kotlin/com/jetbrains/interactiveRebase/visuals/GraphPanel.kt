package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.UIUtil
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.GraphInfo
import com.jetbrains.interactiveRebase.listeners.RebaseDragAndDropListener
import com.jetbrains.interactiveRebase.services.ModelService
import java.awt.*
import java.awt.geom.CubicCurve2D
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
    lateinit var rebaseCircleInAddedBranch: CirclePanel

    var mainBranchPanel: LabeledBranchPanel =
        createLabeledBranchPanel(
            graphInfo.mainBranch,
            SwingConstants.RIGHT,
            mainTheme,
        )

    var addedBranchPanel: LabeledBranchPanel? = null
    var lineOffset = mainBranchPanel.branchPanel.diameter * 2

    init {
        if (graphInfo.addedBranch != null) {
            addedBranchPanel =
                createLabeledBranchPanel(
                    graphInfo.addedBranch!!,
                    SwingConstants.LEFT,
                    addedTheme,
                )
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
        if (project.service<ModelService>().getCurrentCommits().isEmpty()) {
            var message = "No commits to display, please check out a different branch"

            if (!project.service<ModelService>().fetched) {
                message = "Fetching commits"
            }
            val label = JBLabel(message)
            label.setComponentStyle(UIUtil.ComponentStyle.LARGE)
            add(label)

            return
        }

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
    fun computeVerticalOffsets(): Pair<Int, Int> {
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
        if (graphInfo.addedBranch == null || graphInfo.addedBranch?.baseCommit == null) {
            return 0
        }
        val mainCircleCount = mainBranchPanel.branchPanel.circles.size
        val addedCircleCount =
            (graphInfo.addedBranch?.currentCommits?.indexOf(graphInfo.addedBranch?.baseCommit!!) ?: 0) + 1
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
        makeBranchNamePanelDraggable()
    }

    /**
     * Adds a drag and drop listener to the branch name panel
     * to make it support rebasing on top of another branch
     * iff there's a second branch added to the view
     */
    private fun GraphPanel.makeBranchNamePanelDraggable() {
        if (addedBranchPanel != null) {
            val rebaseDragAndDropListener =
                RebaseDragAndDropListener(
                    project,
                    mainBranchPanel.branchNamePanel,
                    addedBranchPanel!!.branchNamePanel,
                    this,
                )
            mainBranchPanel.branchNamePanel.addMouseListener(rebaseDragAndDropListener)
            mainBranchPanel.branchNamePanel.addMouseMotionListener(rebaseDragAndDropListener)
        }
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
        gbc.weightx = 0.5
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
        gbc.weightx = 0.5 // if (graphInfo.addedBranch != null) 0.3 else 0.0
        gbc.weighty = 1.0
        gbc.anchor = GridBagConstraints.NORTH

        if (graphInfo.mainBranch.isPrimary) {
            gbc.fill = GridBagConstraints.HORIZONTAL
        } else {
            gbc.fill = GridBagConstraints.BOTH
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

        if (mainBranchPanel.branchPanel.circles.isEmpty()) return
        // Coordinates of the last circle of the main branch
        val (mainCircleCenterX, mainCircleCenterY) = centerCoordinatesOfLastMainCircle()

        // Coordinates of the last circle of the added branch
        if (addedBranchPanel != null) {

            rebaseCircleInAddedBranch = addedBranchPanel?.branchPanel?.circles
                ?.firstOrNull { c -> c.commit == graphInfo.addedBranch?.baseCommit }
                ?: mainBranchPanel.branchPanel.circles[0]
            var (addedCircleCenterX, addedCircleCenterY) =
                centerCoordinatesOfBaseCircleInAddedBranch()


            if (Point(mainCircleCenterX, mainCircleCenterY) ==
                Point(addedCircleCenterX, addedCircleCenterY)
            ) {
                addedCircleCenterX++
                addedCircleCenterY++
            }

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
                    mainCircleCenterY.toFloat() + lineOffset,
                    mainCircleCenterX.toFloat(),
                    mainCircleCenterY.toFloat() + lineOffset,
                    addedCircleCenterX.toFloat(),
                    addedCircleCenterY.toFloat(),
                )

            // If added branch is not rendered because the screen is too small
            // coordinates appear to be 0
            // Hence, we don't draw the line in this case
            if (addedCircleCenterX != 0 && addedCircleCenterY != 0) {
                g2d.draw(curve)
            }
        }
    }

    /**
     * Find the coordinates of the center
     * of the last circle of the primary (checked out) branch
     */
    fun centerCoordinatesOfBaseCircleInAddedBranch(): Pair<Int, Int> {
        val addedCircleCenterX =
            addedBranchPanel!!.x + addedBranchPanel!!.branchPanel.x + rebaseCircleInAddedBranch.x + rebaseCircleInAddedBranch.width / 2
        val addedCircleCenterY =
            addedBranchPanel!!.y + addedBranchPanel!!.branchPanel.y + rebaseCircleInAddedBranch.y + rebaseCircleInAddedBranch.height / 2
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
        addedBranchPanel = null
        removeAll()

        mainBranchPanel =
            createLabeledBranchPanel(
                graphInfo.mainBranch,
                SwingConstants.RIGHT,
                mainTheme,
            )

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
        colors: Array<Color> = arrayOf(
            mainBranchPanel.branchPanel.circles.last()
                .colorTheme.regularCircleColor,
            addedBranchPanel?.branchPanel!!.circles.last()
                .colorTheme.regularCircleColor,
        )
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
