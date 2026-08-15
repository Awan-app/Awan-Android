package com.awan.feature.calendar.impl.presentation

import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.model.TemplateOverride
import com.awan.app.core.domain.zones.model.WeeklyTemplate
import com.awan.app.core.model.CalendarGoal as CoreCalendarGoal
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

    fun filterAndSortUpcomingGoals(goals: List<CoreCalendarGoal>, today: LocalDate): List<CalendarGoal> =
        goals.asSequence()
            .filter { it.status.equals("ACTIVE", true) && !it.isInbox }
            .mapNotNull { goal -> parseLocalDate(goal.targetDate)?.let { CalendarGoal(goal.id, goal.title, it) } }
            .filter { !it.targetDate.isBefore(today) }
            .sortedBy(CalendarGoal::targetDate)
            .toList()

    fun buildMonthDays(
        yearMonth: YearMonth,
        today: LocalDate,
        selectedDate: LocalDate?,
        streakDates: Set<LocalDate>,
        goalDates: Set<LocalDate>,
        routineDates: Set<LocalDate> = emptySet(),
    ): List<DayState> {
        val offset = yearMonth.atDay(1).dayOfWeek.value % 7
        val start = yearMonth.atDay(1).minusDays(offset.toLong())
        val count = if (offset + yearMonth.lengthOfMonth() > 35) 42 else 35
        return (0 until count).map { index ->
            val date = start.plusDays(index.toLong())
            DayState(
                date = date,
                isCurrentMonth = date.month == yearMonth.month && date.year == yearMonth.year,
                isToday = date == today,
                isSelected = date == selectedDate,
                isStreakDay = date in streakDates,
                hasDeadline = date in goalDates,
                hasRoutine = date in routineDates,
            )
        }
    }

    fun calculateRoutineDates(
        yearMonth: YearMonth,
        templates: List<WeeklyTemplate>,
        overrides: List<TemplateOverride>,
    ): Set<LocalDate> {
        val routineDates = mutableSetOf<LocalDate>()
        val start = yearMonth.atDay(1).minusDays(7) // buffer
        val end = yearMonth.atEndOfMonth().plusDays(7) // buffer

        // 1. Add specific override dates
        overrides.forEach { override ->
            parseLocalDate(override.dateOfDay)?.let { date ->
                if (override.zones.isNotEmpty()) {
                    routineDates.add(date)
                }
            }
        }

        // 2. Add recurring template dates (only if no override exists for that date)
        val overrideDates = overrides.mapNotNull { parseLocalDate(it.dateOfDay) }.toSet()

        var current = start
        while (!current.isAfter(end)) {
            if (current !in overrideDates) {
                val targetDayOfWeek = DayOfWeek.valueOf(current.dayOfWeek.name)
                val hasRoutine = templates.any {
                    it.daysOfWeek.contains(targetDayOfWeek) && it.zones.isNotEmpty()
                }
                if (hasRoutine) {
                    routineDates.add(current)
                }
            }
            current = current.plusDays(1)
        }

        return routineDates
    }

    fun parseZoneIdOrDefault(value: String?): ZoneId = runCatching { ZoneId.of(value.orEmpty().trim()) }.getOrDefault(ZoneId.systemDefault())
}