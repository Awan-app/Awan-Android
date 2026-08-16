package com.awan.feature.calendar.impl.presentation

import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.model.TemplateOverride
import com.awan.app.core.domain.zones.model.WeeklyTemplate
import com.awan.app.core.model.CalendarGoal as CoreCalendarGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
        val monthDays = CalendarDateMapper.buildMonthDays(yearMonth, today, today, emptySet(), emptyList())

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

    // ── calculateRoutineDates ─────────────────────────────────────────────

    /**
     * An empty override (zones = []) on a day that has a recurring template should
     * remove that date from routineDates. This is intentional: the override means
     * "no routine today", effectively clearing the recurring schedule for that date.
     */
    @Test
    fun emptyOverrideRemovesRecurringRoutineForThatDate() {
        // 2026-08-10 is a MONDAY
        val yearMonth = YearMonth.of(2026, 8)
        val template = weeklyTemplate(
            daysOfWeek = listOf(DayOfWeek.MONDAY),
            zones = listOf(sampleZone()),
        )
        // Empty override on a Monday → "no routine that day"
        val override = TemplateOverride(
            id = "override-1",
            dateOfDay = "2026-08-10",
            zones = emptyList(),
        )

        val result = CalendarDateMapper.calculateRoutineDates(yearMonth, listOf(template), listOf(override))
        val overriddenDate = LocalDate.of(2026, 8, 10)

        assertFalse(
            "An empty override should remove the recurring routine for its date",
            overriddenDate in result,
        )
        // Other Mondays in the month should still be marked as routine
        assertTrue(LocalDate.of(2026, 8, 3) in result)
        assertTrue(LocalDate.of(2026, 8, 17) in result)
    }

    /**
     * An override with non-empty zones on a recurring-template day should still
     * mark the date as having a routine (the override's zones replace the template's).
     */
    @Test
    fun overrideWithZonesReplacesRecurringTemplate() {
        val yearMonth = YearMonth.of(2026, 8)
        val template = weeklyTemplate(
            daysOfWeek = listOf(DayOfWeek.MONDAY),
            zones = listOf(sampleZone()),
        )
        val override = TemplateOverride(
            id = "override-1",
            dateOfDay = "2026-08-10",
            zones = listOf(sampleZone(id = "override-zone")),
        )

        val result = CalendarDateMapper.calculateRoutineDates(yearMonth, listOf(template), listOf(override))

        assertTrue(
            "An override with zones should still mark the date as routine",
            LocalDate.of(2026, 8, 10) in result,
        )
    }

    /**
     * When multiple templates share the same day-of-week, the date should still be
     * marked as routine as long as any of those templates has non-empty zones.
     */
    @Test
    fun multipleTemplatesSameDayStillMarksRoutine() {
        val yearMonth = YearMonth.of(2026, 8)
        val emptyTemplate = weeklyTemplate(
            id = "t1",
            daysOfWeek = listOf(DayOfWeek.MONDAY),
            zones = emptyList(),
        )
        val filledTemplate = weeklyTemplate(
            id = "t2",
            daysOfWeek = listOf(DayOfWeek.MONDAY),
            zones = listOf(sampleZone()),
        )

        val result = CalendarDateMapper.calculateRoutineDates(
            yearMonth, listOf(emptyTemplate, filledTemplate), emptyList(),
        )

        assertTrue(
            "At least one template with zones should mark the day as routine",
            LocalDate.of(2026, 8, 3) in result,
        )
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private fun weeklyTemplate(
        id: String = "template-1",
        daysOfWeek: List<DayOfWeek> = emptyList(),
        zones: List<DailyZone> = emptyList(),
    ) = WeeklyTemplate(id = id, name = "Test", daysOfWeek = daysOfWeek, zones = zones)

    private fun sampleZone(id: String = "zone-1") = DailyZone(
        id = id, name = "Focus", startTime = "09:00", endTime = "12:00", color = "#FF0000",
    )
}
