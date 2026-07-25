package com.awan.feature.calendar.impl.model

import java.time.LocalDate

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
