package com.jetbrains.interactiveRebase.visuals

import com.intellij.ui.components.JBPanel
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo

// TODO: placeholder class for drawing a graph of two branches
class GraphPanel(
    private val checkedOutBranch: BranchInfo,
    private val otherBranch: BranchInfo,
) : JBPanel<JBPanel<*>>()
