package com.awan.app.core.designsystem

import androidx.compose.runtime.Immutable

@Immutable
data class ScheduleZone(
    val id: String,
    val categoryId: String,
    val category: TaskCategory,
    val startMinutes: Int,
    val endMinutes: Int,
    val isCollapsed: Boolean = false,
) {
    val startHour: Int get() = startMinutes / 60
    val endHour: Int get() = endMinutes / 60

    companion object {
        fun fromHours(
            id: String,
            categoryId: String,
            category: TaskCategory,
            startHour: Int,
            endHour: Int,
            isCollapsed: Boolean = false,
        ): ScheduleZone = ScheduleZone(
            id = id,
            categoryId = categoryId,
            category = category,
            startMinutes = startHour * 60,
            endMinutes = endHour * 60,
            isCollapsed = isCollapsed,
        )
    }
}
