package com.awan.feature.calendar.impl.presentation

import com.awan.app.core.model.CalendarGoal as CoreCalendarGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class CalendarDateMapperTest {
    @Test
    fun keepsOnlyUpcomingActiveNonInboxDeadlines() {
        val today = LocalDate.of(2026, 7, 24)
        val goals = listOf(
            CoreCalendarGoal("active", "Active", "2026-07-26", "ACTIVE", false),
            CoreCalendarGoal("past", "Past", "2026-07-20", "ACTIVE", false),
            CoreCalendarGoal("inbox", "Inbox", "2026-07-27", "ACTIVE", true),
            CoreCalendarGoal("done", "Done", "2026-07-28", "ACHIEVED", false),
        )

        assertEquals(listOf("active"), CalendarDateMapper.filterAndSortUpcomingGoals(goals, today).map { it.id })
    }

    @Test
    fun parsesVariousIsoDateFormats() {
        assertEquals(LocalDate.of(2026, 7, 26), CalendarDateMapper.parseLocalDate("2026-07-26"))
        assertEquals(LocalDate.of(2026, 7, 26), CalendarDateMapper.parseLocalDate("2026-07-26T14:30:00Z"))
        assertEquals(LocalDate.of(2026, 7, 26), CalendarDateMapper.parseLocalDate("2026-07-26T14:30:00"))
        assertEquals(LocalDate.of(2026, 7, 26), CalendarDateMapper.parseLocalDate("2026-07-26 14:30:00"))
    }

    @Test
    fun buildsMonthDaysCorrectly() {
        val yearMonth = YearMonth.of(2026, 7)
        val today = LocalDate.of(2026, 7, 25)
        val monthDays = CalendarDateMapper.buildMonthDays(yearMonth, today, today, emptySet(), emptySet())

        assertNotNull(monthDays)
        assertEquals(35, monthDays.size)
    }

    @Test
    fun calculatesDeadlineProgressAccurately() {
        val today = LocalDate.of(2026, 7, 20)
        // Created July 10, Target July 30 (duration 20 days, 10 days left -> 0.5f)
        val goalHalf = CalendarGoal("1", "Mid", LocalDate.of(2026, 7, 30), LocalDate.of(2026, 7, 10))
        assertEquals(0.5f, CalendarDateMapper.calculateDeadlineProgress(goalHalf, today), 0.01f)

        // Target is today (0 days left -> 0.0f)
        val goalDue = CalendarGoal("2", "Due", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 10))
        assertEquals(0.0f, CalendarDateMapper.calculateDeadlineProgress(goalDue, today), 0.01f)

        // Just started (full time left -> 1.0f)
        val goalNew = CalendarGoal("3", "New", LocalDate.of(2026, 7, 30), LocalDate.of(2026, 7, 20))
        assertEquals(1.0f, CalendarDateMapper.calculateDeadlineProgress(goalNew, today), 0.01f)
    }

    @Test
    fun calculatesDeadlineProgressWhenCreatedAtIsOnOrEqualToOrAfterTargetDate() {
        val today = LocalDate.of(2026, 7, 20)

        // createdAt equals targetDate (0 days runway) -> 0.0f
        val goalSameDay = CalendarGoal("4", "SameDay", LocalDate.of(2026, 7, 25), LocalDate.of(2026, 7, 25))
        assertEquals(0.0f, CalendarDateMapper.calculateDeadlineProgress(goalSameDay, today), 0.01f)

        // createdAt after targetDate (invalid/inconsistent dates) -> 0.0f
        val goalInconsistent = CalendarGoal("5", "Inconsistent", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 25))
        assertEquals(0.0f, CalendarDateMapper.calculateDeadlineProgress(goalInconsistent, today), 0.01f)
    }
}

