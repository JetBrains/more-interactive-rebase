package com.jetbrains.interactiveRebase.utils

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.dataClasses.GraphInfo
import com.jetbrains.interactiveRebase.services.ActionService

fun Project.takeAction(method: () -> Unit) {
    val actionService = this.service<ActionService>()
    val graphPanel = actionService.mainPanel.graphPanel
    val doRefresh = !actionService.mainPanel.graphPanel.refreshed

    graphPanel.markRefreshedAsTrue()
    try {
        method()
    } finally {
        if (doRefresh) {
            graphPanel.markRefreshedAsFalse()
            graphPanel.updateGraphPanel()
        }
    }
}

fun Project.takeActionWithDeselecting(method: () -> Unit, graphInfo: GraphInfo) {
    val actionService = this.service<ActionService>()
    val graphPanel = actionService.mainPanel.graphPanel
    val doRefresh = !actionService.mainPanel.graphPanel.refreshed

    graphPanel.markRefreshedAsTrue()
    try {
        method()
    } finally {
        graphInfo.mainBranch.clearSelectedCommits()
        graphInfo.addedBranch?.clearSelectedCommits()
        if (doRefresh) {
            graphPanel.markRefreshedAsFalse()
            graphPanel.updateGraphPanel()
        }
    }
}
