package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.jetbrains.interactiveRebase.actions.gitPanel.RebaseActionsGroup
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.dataClasses.GraphInfo
import com.jetbrains.interactiveRebase.listeners.BranchNavigationListener
import com.jetbrains.interactiveRebase.listeners.TextFieldListener
import com.jetbrains.interactiveRebase.services.ModelService
import com.jetbrains.interactiveRebase.visuals.multipleBranches.SidePanel
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.OverlayLayout
import javax.swing.ScrollPaneConstants
import javax.swing.SwingUtilities

class MainPanel(
    private val project: Project,
) :
    JBPanel<JBPanel<*>>(), Disposable {
    internal var commitInfoPanel = CommitInfoPanel(project)
    internal var contentPanel: JBScrollPane
    var sidePanelPane: JBScrollPane
    var sidePanel: SidePanel
    var graphPanel: GraphPanel
    internal val dragPanel: DragPanel = DragPanel()
    private val graphInfoListener: GraphInfo.Listener
    private val branchInfoListener: BranchInfo.Listener
    private val commitInfoListener: CommitInfo.Listener
    private val graphInfo: GraphInfo = project.service<ModelService>().graphInfo
    private val primaryBranchInfo: BranchInfo = graphInfo.mainBranch
    private var addedBranchInfo: BranchInfo? = graphInfo.addedBranch
    private val branchNavigationListener: BranchNavigationListener
    private val graphWrapper = JBPanel<JBPanel<*>>()

    init {
        graphWrapper.layout = OverlayLayout(graphWrapper)

        graphPanel = createGraphPanel()
        contentPanel = createContentPanel()
        sidePanel = SidePanel(project.service<ModelService>().graphInfo.branchList, project)
        sidePanelPane = createSidePanel()

        graphWrapper.add(dragPanel, BorderLayout.CENTER)
        graphWrapper.add(graphPanel, BorderLayout.CENTER)

        this.layout = BorderLayout()
        createMainPanel()

        branchInfoListener =
            object : BranchInfo.Listener {
                override fun onNameChange(newName: String) {
                    if (primaryBranchInfo.name != graphPanel.mainBranchPanel.branchName) {
                        sidePanel.updateBranchNames()
                    }
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

        primaryBranchInfo.addListener(branchInfoListener)
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
        if (addedBranchInfo != null) {
            primaryBranchInfo.isPrimary = true
            addedBranchInfo!!.isWritable = false
        }
        return GraphPanel(
            project,
        )
    }

    /**
     * Creates a content panel.
     * This includes the panel with the main graph and the panel with the help button
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

        val help = HelpPanel(project)
        gbc.insets.left = help.width

        contentPanel.add(
            graphWrapper,
            gbc,
        )
        scrollable.setViewportView(contentPanel)
        scrollable.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
        scrollable.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED)
        gbc.gridx = 1
        gbc.gridy = 0
        gbc.fill = GridBagConstraints.VERTICAL
        gbc.weightx = 0.0
        gbc.weighty = 0.0
        gbc.anchor = GridBagConstraints.SOUTHEAST
        gbc.insets.left = 0
        contentPanel.add(help, gbc)

        contentPanel.addMouseListener(
            object : MouseListener {
                override fun mouseClicked(e: MouseEvent?) {
                    SwingUtilities.invokeLater { requestFocusInWindow() }
                    graphPanel.mainBranchPanel.openTextFields.forEach {
                            textField ->
                        if (textField.isVisible && e?.component !== textField) {
                            if (textField.keyListeners.isEmpty() || textField.keyListeners[0] !is TextFieldListener) return
                            val listener = textField.keyListeners[0] as TextFieldListener
                            listener.processEnter()
                        }
                    }
                }

                override fun mousePressed(e: MouseEvent?) {
                    SwingUtilities.invokeLater { requestFocusInWindow() }
                    if (e != null && e.isPopupTrigger) {
                        invokePopup(e.x, e.y)
                        e.consume()
                    }
                }

                override fun mouseReleased(e: MouseEvent?) {
                    SwingUtilities.invokeLater { requestFocusInWindow() }
                    if (e != null && e.isPopupTrigger) {
                        invokePopup(e.x, e.y)
                        e.consume()
                    }
                }

                override fun mouseEntered(e: MouseEvent?) {
                }

                override fun mouseExited(e: MouseEvent?) {
                }
            },
        )

        return scrollable
    }

    fun createSidePanel(): JBScrollPane {
        val scrollable = JBScrollPane()
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
                sidePanelPane.setVisible(false)
                firstComponent = sidePanelPane
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
        primaryBranchInfo.currentCommits.forEach {
            it.addListener(commitInfoListener)
        }
        addedBranchInfo?.currentCommits?.forEach {
            it.addListener(commitInfoListener)
        }
    }

    /**
     * Shows context menu
     */

    fun invokePopup(
        x: Int,
        y: Int,
    ) {
        val actionManager = ActionManager.getInstance()
        val actionsGroup =
            actionManager.getAction(
                "com.jetbrains.interactiveRebase.actions.gitPanel.RebaseActionsGroup",
            ) as RebaseActionsGroup

        val popupMenu = actionManager.createActionPopupMenu(ActionPlaces.EDITOR_TAB, actionsGroup)
        popupMenu.component.show(this, x, y)
    }

    /**
     * Dispose routine
     */
    override fun dispose() {
    }
}
