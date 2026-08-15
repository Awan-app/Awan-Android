package com.awan.feature.goals.impl.presentation

import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalStatus

enum class GoalSearchType { ALL, TASKS, GOALS }

data class GoalFilters(
    val status: GoalStatus? = null,
    val type: GoalSearchType = GoalSearchType.ALL
)

data class GoalsState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val goals: List<Goal> = emptyList(),
    val inboxTaskCount: Int = 0,
    val searchQuery: String = "",
    val isFilterSheetOpen: Boolean = false,
    val appliedFilters: GoalFilters = GoalFilters(),
    val pendingFilters: GoalFilters = GoalFilters(),
    val deletingGoalId: String? = null,
) {
    val filteredGoals: List<Goal>
        get() {
            return goals
                .filter { goal ->
                    val matchesQuery = if (searchQuery.isBlank()) true else {
                        val titleMatch = goal.title.contains(searchQuery, ignoreCase = true) ||
                                (goal.description?.contains(searchQuery, ignoreCase = true) ?: false)
                        val taskMatch = goal.tasks.any { it.title.contains(searchQuery, ignoreCase = true) }
                        
                        when (appliedFilters.type) {
                            GoalSearchType.ALL -> titleMatch || taskMatch
                            GoalSearchType.TASKS -> taskMatch
                            GoalSearchType.GOALS -> titleMatch
                        }
                    }
                    val matchesStatus = if (appliedFilters.status == null) true else {
                        goal.status == appliedFilters.status
                    }
                    matchesQuery && matchesStatus
                }
                .sortedBy { it.status == GoalStatus.ACHIEVED || it.progress >= 1f }
        }

    val isAnyFilterApplied: Boolean
        get() = appliedFilters.status != null || appliedFilters.type != GoalSearchType.ALL
}
