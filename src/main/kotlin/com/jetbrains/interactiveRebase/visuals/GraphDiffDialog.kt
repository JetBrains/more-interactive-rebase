package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.SideBorder
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.util.preferredHeight
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.GraphInfo
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.ModelService
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.Action
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.ScrollPaneConstants

class GraphDiffDialog(val project: Project) : DialogWrapper(project) {
    private var modelService = project.service<ModelService>()
    private val width = 900
    private val height = 850

    init {
        title = "Compare Interactive Rebase Changes"
        init()
    }

    /**
     * Creates no actions such as cancel or OK
     */
    override fun createActions(): Array<Action> {
        return arrayOf()
    }

    override fun getHelpId(): String {
        return "IRGraphDiff"
    }

    /**
     * Sets the size
     */
    override fun getDimensionServiceKey(): String {
        setSize(width, height)
        return "IRGraphDiffDialog"
    }

    /**
     * Creates the labels that distinguish the current and the initial version
     */
    override fun createTitlePane(): JComponent {
        val titlePanel = JBPanel<JBPanel<*>>()
        titlePanel.layout = BorderLayout()
        val initialLabel = JBLabel("<html><b>Initial state</b></html>")
        val currentLabel = JBLabel("<html><b>Current changes</b></html>")
        titlePanel.add(initialLabel, BorderLayout.WEST)
        titlePanel.add(currentLabel, BorderLayout.EAST)

        titlePanel.setBorder(
            BorderFactory.createCompoundBorder(
                SideBorder(UIUtil.getPanelBackground().darker(), SideBorder.BOTTOM),
                JBUI.Borders.empty(0, 5, 10, 5),
            ),
        )
        return titlePanel
    }

    override fun getPreferredSize(): Dimension {
        return JBDimension(width, height)
    }

    /**
     * Duplicates the current GraphInfo two times and disables the listeners for both.
     * One instance has changes reverted and one reflects the current stages.
     */
    override fun createCenterPanel(): JComponent {
        val actualGraph = modelService.graphInfo

        // Create the graph without changes
        val initialGraph: GraphInfo = modelService.duplicateGraphInfo(actualGraph)
        expandBothBranches(initialGraph)
        revertChangesVisually(initialGraph)
        val initialGraphPanel: GraphPanel = createGraphDisplay(initialGraph)

        initialGraphPanel.preferredSize = JBDimension(260, initialGraphPanel.preferredHeight)

        // Create the graph with current changes
        val currentGraph: GraphInfo = modelService.duplicateGraphInfo(actualGraph)
        expandBothBranches(currentGraph)
        val currentGraphPanel: GraphPanel = createGraphDisplay(currentGraph)

        currentGraphPanel.preferredSize = JBDimension(260, currentGraphPanel.preferredHeight)

        // Make both scrollable
        val initialScrollable = JBScrollPane()
        initialScrollable.setViewportView(initialGraphPanel)
        initialScrollable.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED)
        initialScrollable.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED)

        val currentScrollable = JBScrollPane()
        currentScrollable.setViewportView(currentGraphPanel)
        currentScrollable.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED)
        currentScrollable.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED)

        // Put both in a split view
        val split =
            OnePixelSplitter(false, 0.5f).apply {
                firstComponent = initialScrollable
                secondComponent = currentScrollable
            }
        split.minimumSize = Dimension(300, 820)
        return split
    }

    /**
     * Similar to the reset functionality, resets all changes without affecting the invoker.
     * Do not use to reset changes, this will not reset them from the invoker.
     */
    fun revertChangesVisually(graphInfo: GraphInfo) {
        val primaryBranch = graphInfo.mainBranch
        val addedBranch = graphInfo.addedBranch

        primaryBranch.isRebased = false
        addedBranch?.baseCommit = graphInfo.addedBranch?.currentCommits?.last()

        primaryBranch.initialCommits.forEach {
            project.service<ActionService>().resetCommitInfo(it)
        }
        primaryBranch.currentCommits = primaryBranch.initialCommits.toMutableList()

        if (addedBranch != null) {
            addedBranch.initialCommits.forEach {
                project.service<ActionService>().resetAddedCommitInfo(it)
                it.isCollapsed = false
                it.changes.clear()
            }
            addedBranch.currentCommits = addedBranch.initialCommits.toMutableList()
        }

        primaryBranch.clearSelectedCommits()
        addedBranch?.clearSelectedCommits()
    }

    /**
     * From the given GraphInfo, disables all listeners and creates a panel
     */
    fun createGraphDisplay(graphInfo: GraphInfo): GraphPanel {
        val graphPanel = GraphPanel(project, graphInfo)
        disableLabeledBranchPanel(graphPanel.mainBranchPanel)

        if (graphPanel.addedBranchPanel != null) {
            disableLabeledBranchPanel(graphPanel.addedBranchPanel!!)
        }
        return graphPanel
    }

    /**
     * Makes the panel read-only, also removing listeners of child components.
     */
    fun disableLabeledBranchPanel(labeledBranchPanel: LabeledBranchPanel) {
        removeListeners(labeledBranchPanel.branchNamePanel)
        labeledBranchPanel.commitLabels.forEach {
            removeListeners(it)
        }
        removeListeners(labeledBranchPanel)
        val branchPanel = labeledBranchPanel.branchPanel
        branchPanel.circles.forEach {
            removeListeners(it)
        }
    }

    /**
     * Checks if there is a second branch and expands both if it exists, only primary otherwise
     */
    fun expandBothBranches(graphInfo: GraphInfo) {
        expandCommitsForDisplay(graphInfo.mainBranch)
        if (graphInfo.addedBranch != null) {
            expandCommitsForDisplay(graphInfo.addedBranch!!)
        }
    }

    /**
     * Expands the given branch if it is collapsed. Finds the parent of the collapse first.
     */
    private fun expandCommitsForDisplay(branchInfo: BranchInfo) {
        var collapsedParent: CommitInfo? = null
        branchInfo.currentCommits.forEach {
            if (it.isCollapsed) {
                collapsedParent = it
            }
        }
        if (collapsedParent != null) {
            project.service<ActionService>().expandCollapsedCommits(collapsedParent!!, branchInfo, enableNestedCollapsing = false)
        }
    }

    /**
     * Removes key,mouse,focus, and mouseMotion listeners of the component.
     * Used to create a read-only view of a graph.
     */
    fun removeListeners(component: JComponent) {
        component.keyListeners.forEach {
            component.removeKeyListener(it)
        }
        component.mouseListeners.forEach {
            component.removeMouseListener(it)
        }
        component.focusListeners.forEach {
            component.removeFocusListener(it)
        }
        component.mouseMotionListeners.forEach {
            component.removeMouseMotionListener(it)
        }
    }
}
