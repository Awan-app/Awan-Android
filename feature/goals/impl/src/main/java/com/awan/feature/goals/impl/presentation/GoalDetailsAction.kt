package com.awan.feature.goals.impl.presentation

sealed interface GoalDetailsAction {
    data object Retry : GoalDetailsAction
    data object Back : GoalDetailsAction
    data object DeleteClicked : GoalDetailsAction
    data object EditClicked : GoalDetailsAction
    data object EditDismissed : GoalDetailsAction
    data class GoalUpdated(
        val title: String,
        val description: String?,
        val status: String,
        val targetDate: String?
    ) : GoalDetailsAction
    data class TaskChecked(val taskId: String) : GoalDetailsAction
    data object AddTaskClicked : GoalDetailsAction
    data object AddTaskDismissed : GoalDetailsAction
    data class DeleteTaskClicked(val taskId: String) : GoalDetailsAction
    data class MoveTaskClicked(val taskId: String) : GoalDetailsAction
    data class MoveTaskConfirmed(val goalId: String) : GoalDetailsAction
    data object MoveTaskDismissed : GoalDetailsAction
}
