package com.awan.app.core.designsystem

import androidx.compose.runtime.Immutable

@Immutable
data class ScheduleSession(
    val id: String,
    val zoneId: String,
    val taskId: String,
    val taskTitle: String,
    val startMinutes: Int,
    val durationMinutes: Int,
    val category: TaskCategory,
    val status: TaskStatus = TaskStatus.Pending,
    val isFixed: Boolean = false,
    val points: Int? = null,
)
