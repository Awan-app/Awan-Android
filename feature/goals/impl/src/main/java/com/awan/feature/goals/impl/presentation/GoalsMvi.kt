package com.awan.feature.goals.impl.presentation

import com.awan.app.core.model.Goal

// ── State ────────────────────────────────────────────────────────────────────

data class GoalsState(
    val isLoading: Boolean = true,
    val tab: GoalsTab = GoalsTab.Active,
    val activeGoals: List<Goal> = emptyList(),
    val completedGoals: List<Goal> = emptyList(),
)

enum class GoalsTab { Active, Completed }

// ── Action ───────────────────────────────────────────────────────────────────

sealed interface GoalsAction {
    data class TabSelected(val tab: GoalsTab) : GoalsAction
}
