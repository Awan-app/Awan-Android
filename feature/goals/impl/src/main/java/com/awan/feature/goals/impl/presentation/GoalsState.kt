package com.awan.feature.goals.impl.presentation

import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalStatus

enum class GoalsTab {
    Active, Completed
}

data class GoalsState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    val goals: List<Goal> = emptyList(),
    val searchQuery: String = "",
    val tab: GoalsTab = GoalsTab.Active,
) {
    val activeGoals: List<Goal> = goals.filter { it.status == GoalStatus.ACTIVE }
    val completedGoals: List<Goal> = goals.filter { it.status == GoalStatus.ACHIEVED }

    val filteredGoals: List<Goal>
        get() {
            val baseList = when (tab) {
                GoalsTab.Active -> activeGoals
                GoalsTab.Completed -> completedGoals
            }
            
            return if (searchQuery.isBlank()) {
                baseList
            } else {
                baseList.filter { it.title.contains(searchQuery, ignoreCase = true) }
            }
        }
}
