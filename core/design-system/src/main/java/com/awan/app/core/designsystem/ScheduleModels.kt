package com.awan.app.core.designsystem

import androidx.compose.runtime.Immutable

@Immutable
data class ScheduleCategory(
    val id: String,
    val name: String,
    val icon: String = "",
    val categoryStyle: TaskCategory,
)

@Immutable
data class ScheduleZone(
    val id: String,
    val categoryId: String,
    val category: TaskCategory,
    val startHour: Int,
    val endHour: Int,
    val isCollapsed: Boolean = false,
)

@Immutable
data class ScheduleTask(
    val id: String,
    val title: String,
    val categoryId: String? = null,
    val defaultPoints: Int? = null,
)

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
    val points: Int? = null,
)
