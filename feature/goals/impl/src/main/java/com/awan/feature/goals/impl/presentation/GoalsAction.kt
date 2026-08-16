package com.awan.feature.goals.impl.presentation

import com.awan.app.core.model.GoalStatus

sealed interface GoalsAction {
    data class SearchQueryChanged(val query: String) : GoalsAction
    data object RetryClicked : GoalsAction
    data class GoalClicked(val goalId: String) : GoalsAction
    data class DeleteGoalClicked(val goalId: String) : GoalsAction
    data object DeleteGoalConfirmed : GoalsAction
    data object DeleteGoalCancelled : GoalsAction
    data object FilterClicked : GoalsAction
    data object DismissFilterSheet : GoalsAction
    data object AddGoalClicked : GoalsAction
    data object InboxClicked : GoalsAction
    data class PendingStatusFilterChanged(val status: GoalStatus?) : GoalsAction
    data class PendingTypeFilterChanged(val type: GoalSearchType) : GoalsAction
    data object ApplyFiltersClicked : GoalsAction
    data object ResetFiltersClicked : GoalsAction
    data object ClearFiltersClicked : GoalsAction
}
