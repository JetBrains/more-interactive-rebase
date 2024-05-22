package com.jetbrains.interactiveRebase.visuals

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo
import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.services.RebaseInvoker
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.SwingConstants

class MainPanel(private val project: Project, private val branchInfo: BranchInfo, private val invoker: RebaseInvoker) :
    JBPanel<JBPanel<*>>(), Disposable {
    internal var commitInfoPanel = CommitInfoPanel(project)
    private var contentPanel: JBPanel<JBPanel<*>>
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
            invoker,
            branchInfo,
            Palette.BLUE,
            SwingConstants.RIGHT,
        )
    }

    /**
     * Creates a content panel.
     */
    fun createContentPanel(): JBPanel<JBPanel<*>> {
        val contentPanel = JBPanel<JBPanel<*>>()
        contentPanel.layout = GridBagLayout()

        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.fill = GridBagConstraints.BOTH

        contentPanel.add(
            branchPanel,
            gbc,
        )
        return contentPanel
    }

    /**
     * Initializes the main component.
     */
    fun createMainPanel() {
        val headerPanel = HeaderPanel(this, project, invoker)

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
        branchInfo.commits.forEach {
            it.addListener(commitInfoListener)
        }
    }

    /**
     * Dispose routine
     */
    override fun dispose() {
    }
}
