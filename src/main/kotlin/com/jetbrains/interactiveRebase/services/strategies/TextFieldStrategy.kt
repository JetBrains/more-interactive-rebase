package com.jetbrains.interactiveRebase.services.strategies

import com.jetbrains.interactiveRebase.dataClasses.CommitInfo
import com.jetbrains.interactiveRebase.services.RebaseInvoker

interface TextFieldStrategy {
    fun handleEnter()
}