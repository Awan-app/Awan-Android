package com.awan.app.core.domain.task.parser

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class TaskInputWriterTest {

    /** Wednesday 2026-07-22, 10:00. */
    private val now = LocalDateTime.of(2026, 7, 22, 10, 0)
    private val today: LocalDate = now.toLocalDate()

    private fun parse(input: String) = TaskInputParser.parse(input, now)

    private fun withTime(input: String, moment: LocalDateTime) =
        TaskInputWriter.withTime(input, parse(input), moment, today)

    private fun withDuration(input: String, minutes: Int) =
        TaskInputWriter.withDuration(input, parse(input), minutes)

    @Test
    fun `a time is appended when the sentence had none`() {
        assertEquals(
            "Go Swimming today at 3pm",
            withTime("Go Swimming", LocalDateTime.of(2026, 7, 22, 15, 0)),
        )
    }

    @Test
    fun `an existing time phrase is replaced, not duplicated`() {
        assertEquals(
            "Go Swimming today at 5pm",
            withTime("Go Swimming tomorrow 9am", LocalDateTime.of(2026, 7, 22, 17, 0)),
        )
    }

    @Test
    fun `replacing a time in the middle keeps the words either side`() {
        assertEquals(
            "Go today at 5pm Swimming",
            withTime("Go tomorrow Swimming", LocalDateTime.of(2026, 7, 22, 17, 0)),
        )
    }

    @Test
    fun `a range is replaced as a whole`() {
        assertEquals(
            "Go Swimming today at 8am",
            withTime("Go Swimming from 3pm to 5pm", LocalDateTime.of(2026, 7, 22, 8, 0)),
        )
    }

    @Test
    fun `duration is appended and then replaced`() {
        val once = withDuration("Go Swimming", 90)
        assertEquals("Go Swimming for 1h30", once)
        assertEquals("Go Swimming for 2 hours", withDuration(once, 120))
    }

    @Test
    fun `duration phrasing reads naturally at each scale`() {
        assertEquals("for 45 min", TaskInputWriter.durationPhrase(45))
        assertEquals("for 1 hour", TaskInputWriter.durationPhrase(60))
        assertEquals("for 3 hours", TaskInputWriter.durationPhrase(180))
        assertEquals("for 1h30", TaskInputWriter.durationPhrase(90))
    }

    @Test
    fun `date phrasing degrades from today to weekday to a numeric date`() {
        assertEquals("today at 9am", TaskInputWriter.timePhrase(today.atTime(9, 0), today))
        assertEquals("tomorrow at 9am", TaskInputWriter.timePhrase(today.plusDays(1).atTime(9, 0), today))
        assertEquals("saturday at 9am", TaskInputWriter.timePhrase(today.plusDays(3).atTime(9, 0), today))
        assertEquals("21/8 at 9am", TaskInputWriter.timePhrase(today.plusDays(30).atTime(9, 0), today))
    }

    @Test
    fun `midnight and noon survive the clock phrasing`() {
        assertEquals("today at 12am", TaskInputWriter.timePhrase(today.atTime(0, 0), today))
        assertEquals("today at 12pm", TaskInputWriter.timePhrase(today.atTime(12, 0), today))
    }

    // ── The contract that makes the sentence the source of truth ─────────────

    @Test
    fun `every written time phrase parses back to the same moment`() {
        val moments = listOf(
            today.atTime(15, 0),
            today.atTime(0, 0),
            today.atTime(12, 0),
            today.atTime(9, 30),
            today.plusDays(1).atTime(18, 45),
            today.plusDays(3).atTime(7, 5),
            today.plusDays(30).atTime(21, 0),
        )
        moments.forEach { moment ->
            val text = withTime("Go Swimming", moment)
            val reparsed = parse(text)
            assertEquals("round trip of '$text'", moment, reparsed.startAt)
            assertEquals("title survived '$text'", "Go Swimming", reparsed.title)
        }
    }

    @Test
    fun `every written duration phrase parses back to the same length`() {
        listOf(5, 15, 45, 60, 90, 120, 185, 480).forEach { minutes ->
            val text = withDuration("Go Swimming", minutes)
            val reparsed = parse(text)
            assertEquals("round trip of '$text'", minutes, reparsed.durationMinutes)
            assertEquals("title survived '$text'", "Go Swimming", reparsed.title)
        }
    }

    @Test
    fun `time and duration can both be written without disturbing each other`() {
        val withBoth = withDuration(
            withTime("Go Swimming", today.plusDays(1).atTime(16, 0)),
            150,
        )
        val reparsed = parse(withBoth)

        assertEquals("Go Swimming", reparsed.title)
        assertEquals(today.plusDays(1).atTime(16, 0), reparsed.startAt)
        assertEquals(150, reparsed.durationMinutes)
    }

    @Test
    fun `rewriting one attribute repeatedly does not grow the sentence`() {
        var text = "Go Swimming"
        repeat(5) { step ->
            text = TaskInputWriter.withTime(text, parse(text), today.atTime(10 + step, 0), today)
        }
        assertEquals("Go Swimming today at 2pm", text)
    }
}
