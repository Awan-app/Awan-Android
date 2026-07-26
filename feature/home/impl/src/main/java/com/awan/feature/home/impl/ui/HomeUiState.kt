@file:Suppress("NewApi")

package com.awan.feature.home.impl.ui

import com.awan.app.core.designsystem.CategoryProgressSegment
import com.awan.app.core.designsystem.MascotExpression
import com.awan.app.core.designsystem.ScheduleCategory
import com.awan.app.core.designsystem.ScheduleSession
import com.awan.app.core.designsystem.ScheduleTask
import com.awan.app.core.designsystem.ScheduleZone
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun formatSelectedDate(date: LocalDate): String {
    val today = LocalDate.now()
    val pattern = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)
    return if (date == today) "Today · ${date.format(pattern)}" else date.format(pattern)
}

data class HomeUiState(
    val userName: String = "",
    val greetingPrefix: String = "Good afternoon",
    val streakCount: Int = 0,
    val pointsCount: Int = 0,
    val mascotExpression: MascotExpression = MascotExpression.Idle,
    val subtitleText: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val isToday: Boolean = true,
    val isPastDate: Boolean = false,
    val selectedDateText: String = formatSelectedDate(LocalDate.now()),
    val totalTasksCount: Int = 0,
    val completedSessionsCount: Int = 0,
    val completedHours: Double = 0.0,
    val totalHours: Double = 0.0,
    val scheduledHoursText: String = "",
    val categories: List<ScheduleCategory> = emptyList(),
    val zones: List<ScheduleZone> = emptyList(),
    val tasks: List<ScheduleTask> = emptyList(),
    val sessions: List<ScheduleSession> = emptyList(),
    val progressSegments: List<CategoryProgressSegment> = emptyList(),
    val currentTimeFormatted: String = "",
    val currentTimeMinutes: Int = 0,
    val hasConflict: Boolean = false,
    val conflictMessage: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)
