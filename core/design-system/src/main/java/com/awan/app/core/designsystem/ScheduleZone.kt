package com.awan.app.core.designsystem

import androidx.compose.runtime.Immutable

@Immutable
data class ScheduleZone(
    val id: String,
    val categoryId: String,
    val category: TaskCategory,
    val startHour: Int,
    val endHour: Int,
    val isCollapsed: Boolean = false,
)
