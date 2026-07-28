package com.awan.app.core.domain.task.parser

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

class TaskInputWriterTest {

    /** Wednesday 2026-07-22, 10:00. */
    private val now = LocalDateTime.of(2026, 7, 22, 10, 0)
    private val today: LocalDate = now.toLocalDate()

    /** These phrases are English; [TaskInputWriterArabicTest] covers the other half. */
    private val hostLocale: Locale = Locale.getDefault()

    @Before
    fun setUp() {
        Locale.setDefault(Locale.ENGLISH)
    }

    @After
    fun tearDown() {
        Locale.setDefault(hostLocale)
    }

    private fun parse(input: String) = TaskInputParser.parse(input, now)

    private fun withTime(input: String, moment: LocalDateTime) =
        TaskInputWriter.withTime(input, parse(input), moment, today)

    private fun withDuration(input: String, minutes: Int) =
        TaskInputWriter.withDuration(input, parse(input), minutes)

    private fun withDate(input: String, date: LocalDate) =
        TaskInputWriter.withDate(input, parse(input), date, today)

    private fun withCategory(input: String, categoryName: String) =
        TaskInputWriter.withCategory(input, parse(input), categoryName)

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

    // ── Dates written without a time ─────────────────────────────────────────

    @Test
    fun `a day is appended when the sentence had none`() {
        assertEquals("Go Swimming tomorrow", withDate("Go Swimming", today.plusDays(1)))
    }

    @Test
    fun `moving the day keeps a time the sentence already stated`() {
        assertEquals("Gym saturday at 6pm", withDate("Gym tomorrow at 6pm", today.plusDays(3)))
    }

    @Test
    fun `moving the day does not invent a time the sentence never stated`() {
        assertEquals("Gym saturday", withDate("Gym tomorrow", today.plusDays(3)))
    }

    @Test
    fun `every written day phrase parses back to the same day`() {
        listOf(0L, 1L, 3L, 6L, 7L, 30L).forEach { offset ->
            val date = today.plusDays(offset)
            val text = withDate("Go Swimming", date)
            val reparsed = parse(text)
            assertEquals("round trip of '$text'", date, reparsed.startAt?.toLocalDate())
            assertEquals("title survived '$text'", "Go Swimming", reparsed.title)
        }
    }

    // ── Zones ────────────────────────────────────────────────────────────────

    @Test
    fun `a zone is appended when the sentence had none, and replaced when it had one`() {
        val once = withCategory("Gym", "Play")
        assertEquals("Gym @Play", once)
        assertEquals("Gym @Work", withCategory(once, "Work"))
    }

    @Test
    fun `a single-word zone round-trips through the parser as its own token`() {
        listOf("Study", "Work", "Play", "Personal").forEach { name ->
            val text = withCategory("Gym", name)
            val reparsed = parse(text)
            assertEquals("round trip of '$text'", name.lowercase(), reparsed.categoryToken?.lowercase())
            assertEquals("title survived '$text'", "Gym", reparsed.title)
        }
    }

    @Test
    fun `a multi-word zone is written by its first word so the parser can hold it`() {
        assertEquals("Gym @Deep", withCategory("Gym", "Deep Work"))
        assertEquals("Deep", parse("Gym @Deep").categoryToken)
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
