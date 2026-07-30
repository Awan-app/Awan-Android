package com.awan.feature.goals.impl.presentation

import com.awan.app.core.model.Goal

enum class GoalsTab {
    Active,
    Completed,
}

data class GoalsState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val tab: GoalsTab = GoalsTab.Active,
    val activeGoals: List<Goal> = emptyList(),
    val completedGoals: List<Goal> = emptyList(),
)

sealed interface GoalsAction {
    data class TabSelected(val tab: GoalsTab) : GoalsAction
    data object RetryClicked : GoalsAction
}
