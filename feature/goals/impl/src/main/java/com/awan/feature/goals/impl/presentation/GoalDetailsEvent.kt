package com.awan.feature.goals.impl.presentation

sealed interface GoalDetailsEvent {
    data object NavigateBack : GoalDetailsEvent
}
