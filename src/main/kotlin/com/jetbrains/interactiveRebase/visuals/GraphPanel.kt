package com.jetbrains.interactiveRebase.visuals

import com.intellij.ui.components.JBPanel

// TODO: placeholder class for drawing a graph of two branches
class GraphPanel(
    private val checkedOutBranch: Branch,
    private val otherBranch: Branch,
) : JBPanel<JBPanel<*>>()
