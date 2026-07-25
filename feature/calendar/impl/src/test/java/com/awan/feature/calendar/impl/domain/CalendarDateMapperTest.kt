package com.awan.feature.calendar.impl.domain

import com.awan.app.core.model.Goal
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
            Goal("active", "Active", "2026-07-26", "ACTIVE", false),
            Goal("past", "Past", "2026-07-20", "ACTIVE", false),
            Goal("inbox", "Inbox", "2026-07-27", "ACTIVE", true),
            Goal("done", "Done", "2026-07-28", "ACHIEVED", false),
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

