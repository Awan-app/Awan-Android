package com.awan.feature.goals.impl.presentation

sealed interface GoalDetailsEvent {
    data object NavigateBack : GoalDetailsEvent
    data class ShowError(val message: com.awan.app.core.common.text.UiText) : GoalDetailsEvent
}
