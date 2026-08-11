package com.awan.feature.goals.impl.presentation

sealed interface GoalDetailsAction {
    data object Retry : GoalDetailsAction
    data object Back : GoalDetailsAction
    data object DeleteClicked : GoalDetailsAction
    data object EditClicked : GoalDetailsAction
    data object EditDismissed : GoalDetailsAction
    data class GoalUpdated(
        val title: String,
        val description: String?,
        val status: String,
        val targetDate: String?
    ) : GoalDetailsAction
}
