package com.awan.feature.goals.impl.presentation

sealed interface GoalsEvent {
    data class NavigateToGoalDetails(val goalId: String) : GoalsEvent
}
