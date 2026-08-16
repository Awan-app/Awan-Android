package com.awan.feature.addtask.presentation

sealed interface AddTaskEvent {
    data class TaskCreated(val title: String) : AddTaskEvent
    data class GoalCreated(val title: String) : AddTaskEvent
    data class GoalScheduleRequested(val goalId: String) : AddTaskEvent
    data object Dismissed : AddTaskEvent

    /** Hand-off to Awan's full-screen proposal review. Nothing here has been sent to the backend yet. */
    data class AiRequested(val text: String, val note: String?, val imageUri: String?) : AddTaskEvent
}
