package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.GraphInfo
import com.jetbrains.interactiveRebase.listeners.BranchNavigationListener
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.visuals.multipleBranches.SidePanel
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.ScrollPaneConstants

class MainPanel(
    private val project: Project,
) :
    JBPanel<JBPanel<*>>(), Disposable {
    internal var commitInfoPanel = CommitInfoPanel(project)
    internal var contentPanel: JBScrollPane
    internal var sidePanel: JBScrollPane
    internal var graphPanel: GraphPanel
    private val graphInfoListener: GraphInfo.Listener
    private val branchInfoListener: BranchInfo.Listener
    private val commitInfoListener: CommitInfo.Listener
    private val graphInfo: GraphInfo = project.service<ModelService>().graphInfo
    private val branchInfo: BranchInfo = graphInfo.mainBranch
    private var otherBranchInfo: BranchInfo? = graphInfo.addedBranch
    private val branchNavigationListener: BranchNavigationListener

    init {
        graphPanel = createGraphPanel()
        contentPanel = createContentPanel()
        sidePanel = createSidePanel()

        this.layout = BorderLayout()
        createMainPanel()

        branchInfoListener =
            object : BranchInfo.Listener {
                override fun onNameChange(newName: String) {
                    graphPanel.mainBranchPanel.updateBranchName()
                }

                override fun onCommitChange(commits: List<CommitInfo>) {
                    graphPanel.updateGraphPanel()
                    registerCommitListener()
                }

                override fun onSelectedCommitChange(selectedCommits: MutableList<CommitInfo>) {
                    graphPanel.updateGraphPanel()
                    commitInfoPanel.commitsSelected(selectedCommits.map { it.commit })
                }

                override fun onCurrentCommitsChange(currentCommits: MutableList<CommitInfo>) {
                    graphPanel.updateGraphPanel()
                    registerCommitListener()
                }
            }

        graphInfoListener =
            object : GraphInfo.Listener {
                override fun onBranchChange() {
                    graphPanel.updateGraphPanel()
                    graphInfo.addedBranch?.addListener(branchInfoListener)
                }
            }

        commitInfoListener =
            object : CommitInfo.Listener {
                override fun onCommitChange() {
                    graphPanel.updateGraphPanel()
                }
            }

        graphInfo.addListener(graphInfoListener)
        branchNavigationListener = BranchNavigationListener(project)

        branchInfo.addListener(branchInfoListener)
        registerCommitListener()
        this.addKeyListener(branchNavigationListener)

        Disposer.register(this, branchInfoListener)
        Disposer.register(this, commitInfoListener)
        Disposer.register(this, branchNavigationListener)
    }

    /**
     * Creates a graph panel.
     */
    fun createGraphPanel(): GraphPanel {
        if (otherBranchInfo != null) {
            branchInfo.isPrimary = true
            otherBranchInfo!!.isWritable = false
        }
        return GraphPanel(
            project,
        )
    }

    /**
     * Creates a content panel.
     */
    fun createContentPanel(): JBScrollPane {
        val scrollable = JBScrollPane()

        val contentPanel = JBPanel<JBPanel<*>>()
        contentPanel.layout = GridBagLayout()

        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 1.0
        gbc.weighty = 1.0
        gbc.anchor = GridBagConstraints.CENTER
        gbc.fill = GridBagConstraints.VERTICAL

        contentPanel.add(
            graphPanel,
            gbc,
        )

        scrollable.setViewportView(contentPanel)
        scrollable.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
        scrollable.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED)
        return scrollable
    }

    fun createSidePanel(): JBScrollPane {
        val scrollable = JBScrollPane()
        val sidePanel = SidePanel(project.service<ModelService>().graphInfo.branchList, project)
        scrollable.setViewportView(sidePanel)
        scrollable.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
        scrollable.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED)
        return scrollable
    }

    /**
     * Initializes the main component.
     */
    fun createMainPanel() {
        val headerPanel = HeaderPanel(project)

        val firstDivider =
            OnePixelSplitter(false, 0.7f).apply {
                firstComponent = contentPanel
                secondComponent = commitInfoPanel
            }

        val secondDivider =
            OnePixelSplitter(false, 0.18f).apply {
                sidePanel.setVisible(false)
                firstComponent = sidePanel
                secondComponent = firstDivider
            }
        val thirdDivider =
            OnePixelSplitter(true, 0.03f, 0.03f, 0.03f).apply {
                firstComponent = headerPanel
                secondComponent = secondDivider
            }

        this.add(thirdDivider, BorderLayout.CENTER)
    }

    fun registerCommitListener() {
        branchInfo.currentCommits.forEach {
            it.addListener(commitInfoListener)
        }
        otherBranchInfo?.currentCommits?.forEach {
            it.addListener(commitInfoListener)
        }
    }

    /**
     * Dispose routine
     */
    override fun dispose() {
    }
}
