package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
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
import javax.swing.plaf.basic.BasicSplitPaneDivider
import javax.swing.plaf.basic.BasicSplitPaneUI


class GraphDiffDialog(val project : Project) : DialogWrapper(project) {
    private var diffPanel = JBPanel<JBPanel<*>>()

    init {

        println("in inint")
        setTitle("title")
        val maybe = JBPanel<JBPanel<*>>()
        maybe.layout = BorderLayout()
        maybe.add(JBLabel("maybe this?"))
//        diffPanel.background = Color.GREEN
        init()

//        this.size = Dimension(500, 500)
//        add(maybe)
//        this.contentPane = maybe
    }

    override fun createContentPane(): JComponent {
//        val diffPanel = JBPanel<JBPanel<*>>()
//        diffPanel.size = Dimension(700, 700)
////        diffPanel.border = BorderFactory.createLineBorder(Color.MAGENTA)
//        diffPanel.add(JBLabel("does it show this"))
////        diffPanel.background = Color.MAGENTA
//        diffPanel.preferredSize = Dimension(500, 500)
//        this.diffPanel = diffPanel
        return JBPanel<JBPanel<*>>()
    }

    override fun createActions(): Array<Action> {
        return arrayOf()
    }

    override fun getHelpId(): String? {
        return "graph"
    }
    override fun getDimensionServiceKey(): String? {
        setSize(800, 800)
        return "GraphDiffDialog"
    }

//    override fun createNorthPanel(): JComponent? {
//        val northPanel = JBPanel<JBPanel<*>>()
//        northPanel.add(JBLabel("do you wrok"))
//        return northPanel
//    }

    override fun createTitlePane(): JComponent {
        val titlePanel = JBPanel<JBPanel<*>>()
        titlePanel.layout = BorderLayout()
        val initialLabel = JBLabel("<html><b>Initial state</b></html>")
        val currentLabel = JBLabel("<html><b>Current changes</b></html>")
        titlePanel.add(initialLabel, BorderLayout.WEST)
        titlePanel.add(currentLabel, BorderLayout.EAST)
//        titlePanel.background = UIUtil.getPanelBackground().darker()
        titlePanel.setBorder(
            BorderFactory.createCompoundBorder(
                SideBorder(UIUtil.getPanelBackground().darker(), SideBorder.BOTTOM),
                JBUI.Borders.empty(0, 5, 10, 5)
            )
        )
        return titlePanel
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(8000,8000)
    }
    override fun createCenterPanel(): JComponent {
        val diffPanel = JBPanel<JBPanel<*>>()
        diffPanel.size = Dimension(700, 900)
        diffPanel.layout = GridBagLayout()

        val initialPane = JBPanel<JBPanel<*>>()
        val actualGraph = project.service<ModelService>().graphInfo

        val initialGraph : GraphInfo = duplicateGraphInfo(actualGraph)
        expandCommitsInGraph(initialGraph)
        revertChanges(initialGraph)
        val initialGraphPanel : GraphPanel = createGraphDisplay(initialGraph)

        initialGraphPanel.preferredSize = Dimension(300, 700)


        val currentGraph : GraphInfo = duplicateGraphInfo(actualGraph)
        expandCommitsInGraph(currentGraph)
        val currentGraphPanel : GraphPanel = createGraphDisplay(currentGraph)

        currentGraphPanel.preferredSize = Dimension(300, 700)


        val initialScrollable = JBScrollPane()
        initialScrollable.setViewportView(initialGraphPanel)
        initialScrollable.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED)
        initialScrollable.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED)

        val currentScrollable = JBScrollPane()

        currentScrollable.setViewportView(currentGraphPanel)
        currentScrollable.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED)
        currentScrollable.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED)


        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, initialScrollable, currentScrollable)
        splitPane.resizeWeight = 0.5

        splitPane.dividerSize = 5  // Change the thickness of the divider
        splitPane.setUI(object : BasicSplitPaneUI() {
            override fun createDefaultDivider(): BasicSplitPaneDivider {
                return object : BasicSplitPaneDivider(this) {
                    override fun paint(g: Graphics) {
                        g.color = UIUtil.getPanelBackground().darker()
                        g.fillRect(0, 0, size.width, size.height)
                        super.paint(g)
                    }
                }
            }})

        return splitPane
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

        primaryBranch.currentCommits.forEach {
            project.service<ActionService>().resetCommitInfo(it)
        }

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
//
//        branchInfo.initialCommits.forEach { commit ->
//            if (commit.isCollapsed) {
//                val collapseCommand = commit.changes.filterIsInstance<CollapseCommand>().lastOrNull() as CollapseCommand
//                commit.changes.remove()
//            }
//            commit.isCollapsed = false
////            if (commit.isCollapsed) {
////                collapsedCommit = commit
////            }
//            val collapsed = false
//            commit.changes = commit.changes.filter{!it.instanceOf(CollapseCommand::class)}
//        }
//
//        collapsedCommits.forEach {
//
//        }
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

//    override fun createSouthPanel(): JComponent {
//        val south = super.createSouthPanel()
////        south.border = BorderFactory.createLineBorder(Color.BLUE)
//        return south
//    }
}