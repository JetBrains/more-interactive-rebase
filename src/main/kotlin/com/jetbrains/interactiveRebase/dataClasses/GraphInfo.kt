package com.jetbrains.interactiveRebase.dataClasses

import com.intellij.openapi.Disposable
import com.jetbrains.interactiveRebase.dataClasses.BranchInfo.Listener

data class GraphInfo(var mainBranch: BranchInfo, var addedBranch: BranchInfo? = null) {
    internal var branchList = mutableListOf<String>()
    private val listeners: MutableList<Listener> = mutableListOf()

    internal fun addListener(listener: Listener) = listeners.add(listener)

    internal fun changeAddedBranch(branch: BranchInfo?)  {
        addedBranch = branch
        listeners.forEach { it.onBranchChange() }
    }

    /**
     * Provides a listener
     * for changes in this class
     */
    interface Listener : Disposable {
        fun onBranchChange()

        override fun dispose() {}
    }
}
