package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.SideBorder
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.GraphInfo
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.rd.framework.base.deepClonePolymorphic
import java.awt.*
import javax.swing.*


class GraphDiffDialog(val project : Project) : DialogWrapper(project) {
    private var diffPanel = JBPanel<JBPanel<*>>()

    init {
        setTitle("Compare Interactive Rebase Changes")
        init()
    }


    override fun createActions(): Array<Action> {
        return arrayOf()
    }

    override fun getHelpId(): String {
        return "IRGraphDiff"
    }
    override fun getDimensionServiceKey(): String {
        setSize(500, 800)
        return "IRGraphDiffDialog"
    }

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
                JBUI.Borders.empty(0, 5, 10, 5)
            )
        )
        return titlePanel
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(500,800)
    }
    override fun createCenterPanel(): JComponent {
        val diffPanel = JBPanel<JBPanel<*>>()
        diffPanel.size = Dimension(480, 780)
        diffPanel.layout = GridBagLayout()

        val actualGraph = project.service<ModelService>().graphInfo

        val initialGraph : GraphInfo = duplicateGraphInfo(actualGraph)
        expandCommitsInGraph(initialGraph)
        revertChanges(initialGraph)
        val initialGraphPanel : GraphPanel = createGraphDisplay(initialGraph)

        initialGraphPanel.preferredSize = Dimension(240, 380)


        val currentGraph : GraphInfo = duplicateGraphInfo(actualGraph)
        expandCommitsInGraph(currentGraph)
        val currentGraphPanel : GraphPanel = createGraphDisplay(currentGraph)

        currentGraphPanel.preferredSize = Dimension(240, 380)

        val initialScrollable = JBScrollPane()
        initialScrollable.setViewportView(initialGraphPanel)
        initialScrollable.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED)
        initialScrollable.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED)

        val currentScrollable = JBScrollPane()

        currentScrollable.setViewportView(currentGraphPanel)
        currentScrollable.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED)
        currentScrollable.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED)

        val split = OnePixelSplitter(false, 0.5f).apply {
            firstComponent = initialScrollable
            secondComponent = currentScrollable
        }
        return split
    }


    private fun expandCommitsInGraph(graphInfo: GraphInfo) {
        expandCommits(graphInfo.mainBranch)
        if (graphInfo.addedBranch != null) {
            expandCommits(graphInfo.addedBranch!!)
        }
    }

    private fun revertChanges(graphInfo: GraphInfo) {
        val primaryBranch = graphInfo.mainBranch
        val addedBranch = graphInfo.addedBranch

        primaryBranch.isRebased = false
        addedBranch?.baseCommit = graphInfo.addedBranch?.currentCommits?.last()

        primaryBranch.initialCommits.forEach {
            project.service<ActionService>().resetCommitInfo(it)
        }
        primaryBranch.currentCommits = primaryBranch.initialCommits.toMutableList()

        primaryBranch.clearSelectedCommits()
        addedBranch?.clearSelectedCommits()
    }

    fun createGraphDisplay(graphInfo : GraphInfo) : GraphPanel{
        val graphPanel = GraphPanel(project, graphInfo)
        disableLabeledBranchPanel(graphPanel.mainBranchPanel)

        if (graphPanel.addedBranchPanel != null) {
            disableLabeledBranchPanel(graphPanel.addedBranchPanel!!)
        }
        return graphPanel
    }

    fun duplicateGraphInfo(graphReference : GraphInfo) : GraphInfo {
        return graphReference.copy(
            mainBranch = duplicateBranchInfo(graphReference.mainBranch),
            addedBranch = if (graphReference.addedBranch == null)  null else duplicateBranchInfo(graphReference.addedBranch!!)
        )
    }

    fun duplicateBranchInfo(branchReference: BranchInfo) : BranchInfo {
        val copy = branchReference.copy(
            initialCommits = branchReference.initialCommits.map { duplicateCommitInfo(it) },
        )
        copy.baseCommit = branchReference.baseCommit
        copy.currentCommits = branchReference.currentCommits.map{duplicateCommitInfo(it)}.toMutableList()
        return copy
    }

    fun duplicateCommitInfo(commitReference: CommitInfo) : CommitInfo {
        return commitReference.copy(
            isSelected = false,
            isHovered = false,
            isDragged = false,
            isCollapsed = commitReference.isCollapsed,
            changes = commitReference.changes.deepClonePolymorphic()
        )
    }

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

    fun expandCommits(branchInfo : BranchInfo) {
        var collapsedParent : CommitInfo? = null

        branchInfo.currentCommits.forEach {
            if (it.isCollapsed) {
                collapsedParent = it
            }
        }

        if (collapsedParent != null) {

            project.service<ActionService>().expandCollapsedCommits(collapsedParent!!, branchInfo)
        }
    }

    fun removeListeners(component : JComponent) {
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

    override fun getPreferredFocusedComponent(): JComponent? {
        return diffPanel
    }

}