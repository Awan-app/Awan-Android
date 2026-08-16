package com.awan.feature.calendar.impl.presentation

import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.model.TemplateOverride
import com.awan.app.core.domain.zones.model.WeeklyTemplate
import com.awan.app.core.model.CalendarGoal as CoreCalendarGoal
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object CalendarDateMapper {

    fun calculateStreakDates(streakCount: Int, today: LocalDate): Set<LocalDate> =
        (0 until streakCount.coerceAtLeast(0)).map { today.minusDays(it.toLong()) }.toSet()

    fun parseLocalDate(value: String?): LocalDate? = runCatching {
        val text = value?.trim().orEmpty()
        if (text.isEmpty()) return null
        val datePart = text.split("T", " ").first()
        LocalDate.parse(datePart)
    }.getOrNull()

    fun calculateDeadlineProgress(goal: CalendarGoal, today: LocalDate): Float {
        val start = goal.createdAt ?: goal.targetDate.minusDays(30)
        if (!start.isBefore(goal.targetDate)) {
            return 0f
        }
        val fullDuration = ChronoUnit.DAYS.between(start, goal.targetDate)
        val timeLeft = ChronoUnit.DAYS.between(today, goal.targetDate).coerceAtLeast(0L)
        return (timeLeft.toFloat() / fullDuration.toFloat()).coerceIn(0f, 1f)
    }

    fun filterAndSortUpcomingGoals(
        goals: List<CoreCalendarGoal>,
        today: LocalDate,
    ): List<CalendarGoal> =
        goals.asSequence()
            .filter { it.status.equals("ACTIVE", true) && !it.isInbox }
            .mapNotNull { goal ->
                val target = parseLocalDate(goal.targetDate) ?: return@mapNotNull null
                val created = parseLocalDate(goal.createdAt)
                CalendarGoal(goal.id, goal.title, target, created)
            }
            .filter { !it.targetDate.isBefore(today) }
            .sortedBy(CalendarGoal::targetDate)
            .toList()

    fun buildMonthDays(
        yearMonth: YearMonth,
        today: LocalDate,
        selectedDate: LocalDate?,
        streakDates: Set<LocalDate>,
        goals: List<CalendarGoal>,
        routineDates: Set<LocalDate> = emptySet(),
    ): List<DayState> {
        val offset = yearMonth.atDay(1).dayOfWeek.value % 7
        val start = yearMonth.atDay(1).minusDays(offset.toLong())
        val count = if (offset + yearMonth.lengthOfMonth() > 35) 42 else 35
        val goalsByDate = goals.groupBy { it.targetDate }

        return (0 until count).map { index ->
            val date = start.plusDays(index.toLong())
            val dayGoals = goalsByDate[date].orEmpty()
            val hasDeadline = dayGoals.isNotEmpty() && !date.isBefore(today)
            val progress = if (hasDeadline) {
                dayGoals.minOfOrNull { calculateDeadlineProgress(it, today) } ?: 0.5f
            } else {
                null
            }

            DayState(
                date = date,
                isCurrentMonth = date.month == yearMonth.month && date.year == yearMonth.year,
                isToday = date == today,
                isSelected = date == selectedDate,
                isStreakDay = date in streakDates,
                hasDeadline = hasDeadline,
                hasRoutine = date in routineDates,
                deadlineProgress = progress,
            )
        }
    }

    fun buildMonthDays(
        yearMonth: YearMonth,
        today: LocalDate,
        selectedDate: LocalDate?,
        streakDates: Set<LocalDate>,
        goalDates: Set<LocalDate>,
        routineDates: Set<LocalDate> = emptySet(),
    ): List<DayState> {
        val goals = goalDates.map { CalendarGoal(id = it.toString(), title = "", targetDate = it) }
        return buildMonthDays(
            yearMonth = yearMonth,
            today = today,
            selectedDate = selectedDate,
            streakDates = streakDates,
            goals = goals,
            routineDates = routineDates,
        )
    }

    fun calculateRoutineDates(
        yearMonth: YearMonth,
        templates: List<WeeklyTemplate>,
        overrides: List<TemplateOverride>,
    ): Set<LocalDate> {
        val routineDates = mutableSetOf<LocalDate>()
        val start = yearMonth.atDay(1).minusDays(7)
        val end = yearMonth.atEndOfMonth().plusDays(7)

        overrides.forEach { override ->
            parseLocalDate(override.dateOfDay)?.let { date ->
                if (override.zones.isNotEmpty()) {
                    routineDates.add(date)
                }
            }
        }

        val overrideDates = overrides.mapNotNull { parseLocalDate(it.dateOfDay) }.toSet()

        var current = start
        while (!current.isAfter(end)) {
            if (current !in overrideDates) {
                val targetDayOfWeek = DayOfWeek.valueOf(current.dayOfWeek.name)
                val template = templates.find { it.daysOfWeek.contains(targetDayOfWeek) }
                if (template != null && template.zones.isNotEmpty()) {
                    routineDates.add(current)
                }
            }
            current = current.plusDays(1)
        }

        return routineDates
    }

    fun parseZoneIdOrDefault(value: String?): ZoneId =
        runCatching { ZoneId.of(value.orEmpty().trim()) }.getOrDefault(ZoneId.systemDefault())
}