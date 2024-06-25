package com.jetbrains.interactiveRebase.utils

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.interactiveRebase.services.ActionService
import com.jetbrains.interactiveRebase.services.ModelService

fun Project.takeAction(method: () -> Unit) {
        val actionService = this.service<ActionService>()
        val graphPanel = actionService.mainPanel.graphPanel
        val doRefresh = !actionService.mainPanel.graphPanel.refreshed

        graphPanel.markRefreshedAsTrue()
        try {
            method()
        } finally {
            if(doRefresh){
            graphPanel.markRefreshedAsFalse()
            graphPanel.updateGraphPanel()}
        }
}

fun Project.takeActionWithDeselecting(method: () -> Unit) {
    val actionService = this.service<ActionService>()
    val modelService = this.service<ModelService>()
    val graphPanel = actionService.mainPanel.graphPanel
    val doRefresh = !actionService.mainPanel.graphPanel.refreshed

    graphPanel.markRefreshedAsTrue()
    try {
        method()
    } finally {
        modelService.branchInfo.clearSelectedCommits()
        modelService.graphInfo.addedBranch?.clearSelectedCommits()
        if(doRefresh){
            graphPanel.markRefreshedAsFalse()
            graphPanel.updateGraphPanel()}
    }
}
