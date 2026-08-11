package com.awan.feature.goals.impl.presentation

import com.awan.app.core.model.Goal

data class GoalDetailsState(
    val isLoading: Boolean = true,
    val goal: Goal? = null,
    val error: String? = null,
)

sealed interface GoalDetailsAction {
    data object Retry : GoalDetailsAction
    data object Back : GoalDetailsAction
}
