package com.awan.app.core.domain.task.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class TaskInputParserTest {

    /** Wednesday 2026-07-22, 10:00. */
    private val now = LocalDateTime.of(2026, 7, 22, 10, 0)

    private fun parse(input: String) = TaskInputParser.parse(input, now)

    @Test
    fun `a bare title stays unscheduled`() {
        val result = parse("Buy groceries")

        assertEquals("Buy groceries", result.title)
        assertNull(result.startAt)
        assertNull(result.durationMinutes)
        assertNull(result.zoneToken)
        assertTrue(result.tokens.isEmpty())
    }

    @Test
    fun `blank input parses to empty`() {
        assertEquals(ParsedTaskInput.Empty, parse("   "))
    }

    @Test
    fun `tomorrow plus a meridiem time`() {
        val result = parse("Gym session tomorrow 6pm")

        assertEquals("Gym session", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 23, 18, 0), result.startAt)
    }

    @Test
    fun `a date without a time defaults to 9am`() {
        val result = parse("Call the dentist tomorrow")

        assertEquals("Call the dentist", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 23, 9, 0), result.startAt)
    }

    @Test
    fun `tonight defaults to the evening rather than 9am`() {
        val result = parse("Read a chapter tonight")

        assertEquals("Read a chapter", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 22, 20, 0), result.startAt)
    }

    @Test
    fun `a weekday resolves to the next occurrence`() {
        val result = parse("Team sync mon 3 pm")

        assertEquals("Team sync", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 27, 15, 0), result.startAt)
    }

    @Test
    fun `today's weekday means next week, not today`() {
        val result = parse("Retro wed")

        assertEquals("Retro", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 29, 9, 0), result.startAt)
    }

    @Test
    fun `the by prefix is swallowed with the weekday`() {
        val result = parse("Submit writing task by fri 4 pm")

        assertEquals("Submit writing task", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 24, 16, 0), result.startAt)
    }

    @Test
    fun `a 24 hour time is understood and at is swallowed`() {
        val result = parse("Standup at 15:00")

        assertEquals("Standup", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 22, 15, 0), result.startAt)
    }

    @Test
    fun `a time already past today rolls to tomorrow`() {
        val result = parse("Water plants 8am")

        assertEquals("Water plants", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 23, 8, 0), result.startAt)
    }

    @Test
    fun `a time still ahead today stays today`() {
        val result = parse("Water plants 8pm")

        assertEquals(LocalDateTime.of(2026, 7, 22, 20, 0), result.startAt)
    }

    @Test
    fun `noon and midnight are understood`() {
        assertEquals(LocalDateTime.of(2026, 7, 22, 12, 0), parse("Lunch noon").startAt)
        assertEquals(LocalDateTime.of(2026, 7, 23, 0, 0), parse("Backup midnight").startAt)
    }

    @Test
    fun `12am is midnight and 12pm is noon`() {
        assertEquals(LocalDateTime.of(2026, 7, 23, 0, 0), parse("Backup 12am").startAt)
        assertEquals(LocalDateTime.of(2026, 7, 22, 12, 0), parse("Lunch 12pm").startAt)
    }

    @Test
    fun `durations in minutes and hours`() {
        assertEquals(45, parse("Read docs for 45m").durationMinutes)
        assertEquals(120, parse("Deep work for 2 hours").durationMinutes)
        assertEquals(90, parse("Workout 1h30").durationMinutes)
        assertEquals(60, parse("Workout 1h").durationMinutes)
    }

    @Test
    fun `a duration does not swallow a meridiem time`() {
        val result = parse("Gym 7am for 45m")

        assertEquals("Gym", result.title)
        assertEquals(45, result.durationMinutes)
        assertEquals(LocalDateTime.of(2026, 7, 23, 7, 0), result.startAt)
    }

    @Test
    fun `a zone token is extracted and removed from the title`() {
        val result = parse("Study chapter 4 @study")

        assertEquals("Study chapter 4", result.title)
        assertEquals("study", result.zoneToken)
    }

    @Test
    fun `an unknown zone token still parses`() {
        assertEquals("nosuchzone", parse("Task @nosuchzone").zoneToken)
    }

    @Test
    fun `every token kind at once`() {
        val result = parse("Study chapter 4 @study mon 4pm for 90m")

        assertEquals("Study chapter 4", result.title)
        assertEquals("study", result.zoneToken)
        assertEquals(90, result.durationMinutes)
        assertEquals(LocalDateTime.of(2026, 7, 27, 16, 0), result.startAt)
        assertEquals(
            setOf(TaskTokenKind.ZONE, TaskTokenKind.DURATION, TaskTokenKind.DATE_TIME),
            result.tokens.map { it.kind }.toSet(),
        )
    }

    @Test
    fun `input that is only tokens leaves an empty title`() {
        val result = parse("tomorrow 6pm @play")

        assertEquals("", result.title)
        assertEquals("play", result.zoneToken)
    }

    @Test
    fun `token ranges point at the matched text in the raw input`() {
        val input = "Gym @play"
        val result = TaskInputParser.parse(input, now)

        val zone = result.tokens.single { it.kind == TaskTokenKind.ZONE }
        assertEquals("@play", input.substring(zone.range.first, zone.range.last + 1))
    }

    @Test
    fun `token ranges never overlap`() {
        val result = parse("Study chapter 4 @study mon 4pm for 90m")
        val sorted = result.tokens.sortedBy { it.range.first }

        sorted.zipWithNext { left, right ->
            assertTrue("$left overlaps $right", left.range.last < right.range.first)
        }
    }

    // ── Time ranges ──────────────────────────────────────────────────────────

    @Test
    fun `from X to Y sets both the start and the length`() {
        val result = parse("Go Swimming from 3pm to 5pm")

        assertEquals("Go Swimming", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 22, 15, 0), result.startAt)
        assertEquals(120, result.durationMinutes)
    }

    @Test
    fun `a range tints only the two clocks, not the joining words`() {
        val input = "Go Swimming from 3pm to 5pm"
        val token = TaskInputParser.parse(input, now).tokens.single { it.kind == TaskTokenKind.DATE_TIME }

        assertEquals("from 3pm to 5pm", input.substring(token.range.first, token.range.last + 1))
        assertEquals(
            listOf("3pm", "5pm"),
            token.highlights.map { input.substring(it.first, it.last + 1) },
        )
    }

    @Test
    fun `range separators and formats`() {
        assertEquals(120, parse("Swim 3pm-5pm").durationMinutes)
        assertEquals(120, parse("Swim 3 to 5pm").durationMinutes)
        assertEquals(120, parse("Swim from 15:00 to 17:00").durationMinutes)
        assertEquals(90, parse("Swim 3pm until 4:30pm").durationMinutes)
        assertEquals(60, parse("Swim 3pm till 4pm").durationMinutes)
    }

    @Test
    fun `a bare left side borrows the right side's meridiem`() {
        val result = parse("Swim 3-5pm")

        assertEquals(LocalDateTime.of(2026, 7, 22, 15, 0), result.startAt)
        assertEquals(120, result.durationMinutes)
    }

    @Test
    fun `a range crossing midnight is a real span, not a negative one`() {
        assertEquals(180, parse("Shift 10pm to 1am").durationMinutes)
    }

    @Test
    fun `an ambiguous numeric range is left in the title`() {
        val result = parse("Read chapter 3 to 5")

        assertEquals("Read chapter 3 to 5", result.title)
        assertNull(result.startAt)
    }

    @Test
    fun `a range combines with a separately named day`() {
        val result = parse("Go Swimming from 3pm to 5pm tomorrow")

        assertEquals("Go Swimming", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 23, 15, 0), result.startAt)
        assertEquals(120, result.durationMinutes)
    }

    // ── Other relative patterns ──────────────────────────────────────────────

    @Test
    fun `in N units is measured from now`() {
        assertEquals(LocalDateTime.of(2026, 7, 22, 10, 30), parse("Call back in 30 minutes").startAt)
        assertEquals(LocalDateTime.of(2026, 7, 22, 12, 0), parse("Call back in 2 hours").startAt)
        assertEquals(LocalDateTime.of(2026, 7, 25, 10, 0), parse("Call back in 3 days").startAt)
        assertEquals("Call back", parse("Call back in 2 hours").title)
    }

    @Test
    fun `in N hours is not read as a duration`() {
        val result = parse("Call back in 2 hours")

        assertEquals(LocalDateTime.of(2026, 7, 22, 12, 0), result.startAt)
        assertNull(result.durationMinutes)
    }

    @Test
    fun `parts of this day resolve to their usual hour`() {
        assertEquals(LocalDateTime.of(2026, 7, 22, 9, 0), parse("Run this morning").startAt)
        assertEquals(LocalDateTime.of(2026, 7, 22, 14, 0), parse("Run this afternoon").startAt)
        assertEquals(LocalDateTime.of(2026, 7, 22, 19, 0), parse("Run this evening").startAt)
        assertEquals("Run", parse("Run this evening").title)
    }

    @Test
    fun `next week lands a week out`() {
        val result = parse("Plan the sprint next week")

        assertEquals("Plan the sprint", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 29, 9, 0), result.startAt)
    }

    @Test
    fun `a bare day has no explicit time, a clock time does`() {
        assertFalse(parse("Call the dentist tomorrow").hasExplicitTime)
        assertTrue(parse("Call the dentist tomorrow 3pm").hasExplicitTime)
        assertTrue(parse("Swim from 3pm to 5pm").hasExplicitTime)
    }

    @Test
    fun `a nonsense clock time is left in the title instead of being invented`() {
        val result = parse("Ticket 99:99")

        assertEquals("Ticket 99:99", result.title)
        assertNull(result.startAt)
    }
}
