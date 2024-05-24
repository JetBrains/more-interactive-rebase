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
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.ScrollPaneConstants
import javax.swing.SwingConstants

class MainPanel(
    private val project: Project,
    private val branchInfo: BranchInfo,
    private val invoker: RebaseInvoker = project.service<RebaseInvoker>(),
) :
    JBPanel<JBPanel<*>>(), Disposable {
    internal var commitInfoPanel = CommitInfoPanel(project)
    private var contentPanel: JBScrollPane
    internal var branchPanel: LabeledBranchPanel
    private val branchInfoListener: BranchInfo.Listener
    private val commitInfoListener: CommitInfo.Listener

    init {
        branchPanel = createBranchPanel()
        contentPanel = createContentPanel()

        this.layout = BorderLayout()
        createMainPanel()

        branchInfoListener =
            object : BranchInfo.Listener {
                override fun onNameChange(newName: String) {
                    branchPanel.updateBranchName()
                }

                override fun onCommitChange(commits: List<CommitInfo>) {
                    branchPanel.updateCommits()
                    registerCommitListener()
                }

                override fun onSelectedCommitChange(selectedCommits: MutableList<CommitInfo>) {
                    branchPanel.updateCommits()
                    commitInfoPanel.commitsSelected(selectedCommits.map { it.commit })
                }

                override fun onCurrentCommitsChange(currentCommits: MutableList<CommitInfo>) {
                    branchPanel.updateCommits()
                    registerCommitListener()
                }
            }

        commitInfoListener =
            object : CommitInfo.Listener {
                override fun onCommitChange() {
                    branchPanel.updateCommits()
                }
            }

        branchInfo.addListener(branchInfoListener)
        registerCommitListener()

        Disposer.register(this, branchInfoListener)
        Disposer.register(this, commitInfoListener)
    }

    /**
     * Creates a branch panel.
     */
    fun createBranchPanel(): LabeledBranchPanel {
        return LabeledBranchPanel(
            project,
            invoker,
            branchInfo,
            Palette.BLUE,
            SwingConstants.RIGHT,
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
        gbc.anchor = GridBagConstraints.CENTER

        contentPanel.add(
            branchPanel,
            gbc,
        )

        scrollable.setViewportView(contentPanel)
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
            OnePixelSplitter(true, 0.03f, 0.03f, 0.03f).apply {
                firstComponent = headerPanel
                secondComponent = firstDivider
            }

        this.add(secondDivider, BorderLayout.CENTER)
    }

    fun registerCommitListener() {
        branchInfo.currentCommits.forEach {
            it.addListener(commitInfoListener)
        }
    }

    /**
     * Dispose routine
     */
    override fun dispose() {
    }
}
