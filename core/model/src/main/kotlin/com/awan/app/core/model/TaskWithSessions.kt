package com.awan.app.core.model

data class TaskWithSessions(
    val task: Task,
    val sessions: List<TaskSession> = emptyList(),
)
