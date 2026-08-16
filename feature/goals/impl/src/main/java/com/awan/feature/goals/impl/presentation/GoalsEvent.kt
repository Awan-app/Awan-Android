package com.awan.feature.goals.impl.presentation

sealed interface GoalsEvent {
    data class NavigateToGoalDetails(val goalId: String) : GoalsEvent
    data object NavigateToAddGoal : GoalsEvent
    data object OpenMenu : GoalsEvent
    data object NavigateToInbox : GoalsEvent
}
