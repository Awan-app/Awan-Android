package com.awan.feature.addtask.presentation

sealed interface AddTaskEvent {
    data class TaskCreated(val title: String) : AddTaskEvent
    data object Dismissed : AddTaskEvent
}
