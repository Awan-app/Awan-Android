package com.awan.feature.goals.impl.presentation

sealed interface GoalsAction {
    data class SearchQueryChanged(val query: String) : GoalsAction
    data object RetryClicked : GoalsAction
    data class GoalClicked(val goalId: String) : GoalsAction
    data class TabSelected(val tab: GoalsTab) : GoalsAction
}
