package com.awan.feature.calendar.impl.presentation

import com.awan.app.core.model.Goal
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

object CalendarDateMapper {
    fun calculateStreakDates(streakCount: Int, today: LocalDate): Set<LocalDate> =
        (0 until streakCount.coerceAtLeast(0)).map { today.minusDays(it.toLong()) }.toSet()

    fun parseLocalDate(value: String?): LocalDate? = runCatching {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) return null
        val datePart = text.split("T", " ").first()
        LocalDate.parse(datePart)
    }.getOrNull()

    fun filterAndSortUpcomingGoals(goals: List<Goal>, today: LocalDate): List<CalendarGoal> =
        goals.asSequence()
            .filter { it.status.equals("ACTIVE", true) && !it.isInbox }
            .mapNotNull { goal -> parseLocalDate(goal.targetDate)?.let { CalendarGoal(goal.id, goal.title, it) } }
            .filter { !it.targetDate.isBefore(today) }
            .sortedBy(CalendarGoal::targetDate)
            .toList()

    fun buildMonthDays(yearMonth: YearMonth, today: LocalDate, selectedDate: LocalDate?, streakDates: Set<LocalDate>, goalDates: Set<LocalDate>): List<DayState> {
        val offset = yearMonth.atDay(1).dayOfWeek.value % 7
        val start = yearMonth.atDay(1).minusDays(offset.toLong())
        val count = if (offset + yearMonth.lengthOfMonth() > 35) 42 else 35
        return (0 until count).map { index ->
            val date = start.plusDays(index.toLong())
            DayState(date, date.month == yearMonth.month && date.year == yearMonth.year, date == today, date == selectedDate, date in streakDates, date in goalDates)
        }
    }

    fun parseZoneIdOrDefault(value: String?): ZoneId = runCatching { ZoneId.of(value.orEmpty().trim()) }.getOrDefault(ZoneId.systemDefault())
}

