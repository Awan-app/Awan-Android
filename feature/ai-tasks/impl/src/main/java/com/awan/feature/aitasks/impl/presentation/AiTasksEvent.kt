package com.awan.feature.aitasks.impl.presentation

sealed interface AiTasksEvent {
    data class TasksCreated(val count: Int) : AiTasksEvent
    data object Dismissed : AiTasksEvent
}
