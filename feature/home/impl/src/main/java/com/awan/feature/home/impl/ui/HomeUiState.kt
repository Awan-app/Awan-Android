@file:Suppress("NewApi")

package com.awan.feature.home.impl.ui

import com.awan.app.core.common.text.UiText
import com.awan.app.core.designsystem.CategoryProgressSegment
import com.awan.app.core.designsystem.MascotExpression
import com.awan.app.core.designsystem.ScheduleCategory
import com.awan.app.core.designsystem.ScheduleSession
import com.awan.app.core.designsystem.ScheduleTask
import com.awan.app.core.designsystem.ScheduleZone
import com.awan.app.core.domain.gamification.model.WheelSegment
import com.awan.feature.home.impl.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.awan.app.core.model.SessionTaskDetail

internal fun formatSelectedDate(date: LocalDate): UiText {
    val today = LocalDate.now()
    val pattern = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    val formatted = date.format(pattern)
    return when (date) {
        today -> UiText.StringResource(R.string.home_date_today_format, formatted)
        today.minusDays(1) -> UiText.StringResource(R.string.home_date_yesterday_format, formatted)
        today.plusDays(1) -> UiText.StringResource(R.string.home_date_tomorrow_format, formatted)
        else -> UiText.DynamicString(formatted)
    }
}

data class SessionDetailDialogState(
    val sessionId: String,
    val isLoading: Boolean = true,
    val detail: SessionTaskDetail? = null,
    val errorMessage: UiText? = null,
    val isEditing: Boolean = false,
    val editTitle: String = "",
    val editDescription: String = "",
    val editDate: LocalDate = LocalDate.now(),
    val editStartMinutes: Int = 0,
    val editEndMinutes: Int = 0,
    val editDurationMinutes: Int = 30,
    val isSaving: Boolean = false,
    val showDeleteConfirmDialog: Boolean = false,
    val isDeleting: Boolean = false,
)

data class HomeUiState(
    val userName: String = "",
    val greetingPrefix: UiText = UiText.StringResource(R.string.home_greeting_afternoon),
    val streakCount: Int = 0,
    val pointsCount: Int = 0,
    val mascotExpression: MascotExpression = MascotExpression.Idle,
    val subtitleText: UiText = UiText.DynamicString(""),
    val selectedDate: LocalDate = LocalDate.now(),
    val isToday: Boolean = true,
    val isPastDate: Boolean = false,
    val selectedDateText: UiText = formatSelectedDate(LocalDate.now()),
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
    val errorMessage: UiText? = null,
    val selectedSessionDetailState: SessionDetailDialogState? = null,
    val hasFreeSpin: Boolean = false,
    val isWheelOpen: Boolean = false,
    val isSpinning: Boolean = false,
    /** Wedges in wheel order; the screen resolves their labels. */
    val wheelSegments: List<WheelSegment> = emptyList(),
    /** Non-null once the server has decided the outcome — the cue for the wheel to land. */
    val landingSegmentId: String? = null,
    val wheelResult: UiText? = null,
)
