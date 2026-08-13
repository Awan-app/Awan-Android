package com.awan.feature.calendar.impl.presentation

import androidx.annotation.StringRes
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class CalendarStreakHeaderState {
    Start,
    Restart,
    Protect,
    Celebrate;

    companion object {
        fun from(streak: Int, maxStreak: Int, isTodayActive: Boolean): CalendarStreakHeaderState {
            val clampedStreak = streak.coerceAtLeast(0)
            val clampedMaxStreak = maxStreak.coerceAtLeast(0)
            return when {
                clampedStreak == 0 && clampedMaxStreak == 0 -> Start
                clampedStreak == 0 -> Restart
                isTodayActive -> Celebrate
                else -> Protect
            }
        }
    }
}

data class CalendarUiState(
    val isLoading: Boolean = true,
    @StringRes val errorMessage: Int? = null,
    val streak: Int = 0,
    val maxStreak: Int = 0,
    val isTodayActive: Boolean = false,
    val streakHeaderState: CalendarStreakHeaderState = CalendarStreakHeaderState.Start,
    val timezone: ZoneId = ZoneId.systemDefault(),
    val today: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val currentYearMonth: YearMonth = YearMonth.now(),
    val streakDates: Set<LocalDate> = emptySet(),
    val upcomingGoals: List<CalendarGoal> = emptyList(),
    val monthDays: List<DayState> = emptyList(),
)

data class CalendarGoal(
    val id: String,
    val title: String,
    val targetDate: LocalDate,
)

data class DayState(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val isStreakDay: Boolean,
    val hasDeadline: Boolean,
)

sealed interface CalendarAction {
    data class SelectDate(val date: LocalDate) : CalendarAction
    data object PreviousMonth : CalendarAction
    data object NextMonth : CalendarAction
    data object Refresh : CalendarAction
}
