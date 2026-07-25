package com.awan.feature.calendar.impl.presentation

import androidx.annotation.StringRes
import com.awan.feature.calendar.impl.model.CalendarGoal
import com.awan.feature.calendar.impl.model.DayState
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class CalendarUiState(
    val isLoading: Boolean = true,
    @StringRes val errorMessage: Int? = null,
    val streak: Int = 0,
    val timezone: ZoneId = ZoneId.systemDefault(),
    val today: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val currentYearMonth: YearMonth = YearMonth.now(),
    val streakDates: Set<LocalDate> = emptySet(),
    val upcomingGoals: List<CalendarGoal> = emptyList(),
    val monthDays: List<DayState> = emptyList(),
)

sealed interface CalendarAction {
    data class SelectDate(val date: LocalDate) : CalendarAction
    data object PreviousMonth : CalendarAction
    data object NextMonth : CalendarAction
    data object Refresh : CalendarAction
}
