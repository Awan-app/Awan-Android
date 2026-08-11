package com.awan.feature.goals.impl.presentation

import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalStatus

data class GoalsState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val goals: List<Goal> = emptyList(),
    val searchQuery: String = "",
) {
    val activeGoals: List<Goal> = goals.filter { it.status == GoalStatus.ACTIVE }

    val filteredGoals: List<Goal>
        get() {
            val baseList = if (searchQuery.isBlank()) {
                activeGoals
            } else {
                activeGoals.filter { it.title.contains(searchQuery, ignoreCase = true) }
            }
            // Sort by priority (Must > Should > Could > Wont)
            return baseList.sortedBy { it.moscowPriority.ordinal }
        }
}

enum class MoscowPriority {
    Must, Should, Could, Wont
}

val Goal.moscowPriority: MoscowPriority
    get() = when {
        title.contains("MUST", ignoreCase = true) || title.contains("مهم", ignoreCase = true) -> MoscowPriority.Must
        title.contains("SHOULD", ignoreCase = true) -> MoscowPriority.Should
        title.contains("COULD", ignoreCase = true) -> MoscowPriority.Could
        title.contains("WONT", ignoreCase = true) -> MoscowPriority.Wont
        else -> MoscowPriority.Could // Default fantasy drift
    }

sealed interface GoalsAction {
    data class SearchQueryChanged(val query: String) : GoalsAction
    data object RetryClicked : GoalsAction
    data class GoalClicked(val goalId: String) : GoalsAction
}

sealed interface GoalsEvent {
    data class NavigateToGoalDetails(val goalId: String) : GoalsEvent
}
