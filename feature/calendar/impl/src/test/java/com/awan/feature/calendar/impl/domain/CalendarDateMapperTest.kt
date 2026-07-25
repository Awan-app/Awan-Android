package com.awan.feature.calendar.impl.domain

import com.awan.app.core.model.Goal
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

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
}
