package com.awan.feature.calendar.impl.presentation

import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalStatus
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
            Goal(id = "active", title = "Active", targetDate = "2026-07-26", status = GoalStatus.ACTIVE, isInbox = false, emoji = "🎯"),
            Goal(id = "past", title = "Past", targetDate = "2026-07-20", status = GoalStatus.ACTIVE, isInbox = false, emoji = "🎯"),
            Goal(id = "inbox", title = "Inbox", targetDate = "2026-07-27", status = GoalStatus.ACTIVE, isInbox = true, emoji = "🎯"),
            Goal(id = "done", title = "Done", targetDate = "2026-07-28", status = GoalStatus.ACHIEVED, isInbox = false, emoji = "🎯"),
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
}
