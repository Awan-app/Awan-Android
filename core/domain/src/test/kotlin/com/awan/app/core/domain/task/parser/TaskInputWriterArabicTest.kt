package com.awan.app.core.domain.task.parser

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

/**
 * The writer's contract in Arabic. The phrase assertions pin what the user sees; the round-trips are
 * the part that actually has to hold — anything written here has to come back off [TaskInputParser]
 * as the same value, or the chips go blank the moment a picker is used.
 */
class TaskInputWriterArabicTest {

    /** Wednesday 2026-07-22, 10:00. */
    private val now = LocalDateTime.of(2026, 7, 22, 10, 0)
    private val today: LocalDate = now.toLocalDate()
    private val arabic: Locale = Locale.forLanguageTag("ar")

    private fun parse(input: String) = TaskInputParser.parse(input, now, arabic)

    private fun withTime(input: String, moment: LocalDateTime) =
        TaskInputWriter.withTime(input, parse(input), moment, today, arabic)

    private fun withDate(input: String, date: LocalDate) =
        TaskInputWriter.withDate(input, parse(input), date, today, arabic)

    private fun withDuration(input: String, minutes: Int) =
        TaskInputWriter.withDuration(input, parse(input), minutes, arabic)

    @Test
    fun `a time is appended when the sentence had none`() {
        assertEquals("سباحة اليوم الساعة 3م", withTime("سباحة", LocalDateTime.of(2026, 7, 22, 15, 0)))
    }

    @Test
    fun `an existing time phrase is replaced, not duplicated`() {
        assertEquals(
            "سباحة اليوم الساعة 5م",
            withTime("سباحة غدا 9ص", LocalDateTime.of(2026, 7, 22, 17, 0)),
        )
    }

    @Test
    fun `a date names the day it is near, and the number when it is not`() {
        assertEquals("اليوم الساعة 9ص", TaskInputWriter.timePhrase(today.atTime(9, 0), today, arabic))
        assertEquals("غدا الساعة 9ص", TaskInputWriter.timePhrase(today.plusDays(1).atTime(9, 0), today, arabic))
        assertEquals("السبت الساعة 9ص", TaskInputWriter.timePhrase(today.plusDays(3).atTime(9, 0), today, arabic))
        assertEquals("21/8 الساعة 9ص", TaskInputWriter.timePhrase(today.plusDays(30).atTime(9, 0), today, arabic))
    }

    @Test
    fun `midnight and noon keep their half of the day`() {
        assertEquals("اليوم الساعة 12ص", TaskInputWriter.timePhrase(today.atTime(0, 0), today, arabic))
        assertEquals("اليوم الساعة 12م", TaskInputWriter.timePhrase(today.atTime(12, 0), today, arabic))
    }

    @Test
    fun `a length is written the way this language says it`() {
        assertEquals("لمدة 45 دقيقة", TaskInputWriter.durationPhrase(45, arabic))
        assertEquals("لمدة 1 ساعة", TaskInputWriter.durationPhrase(60, arabic))
        assertEquals("لمدة 3 ساعات", TaskInputWriter.durationPhrase(180, arabic))
        // No `1س30` shorthand, so an hour and a half is said in minutes.
        assertEquals("لمدة 90 دقيقة", TaskInputWriter.durationPhrase(90, arabic))
    }

    @Test
    fun `moving the day alone keeps a time already in the sentence`() {
        assertEquals(
            "تمرين الجمعة الساعة 6م",
            withDate("تمرين غدا الساعة 6م", LocalDate.of(2026, 7, 24)),
        )
    }

    @Test
    fun `re-picking replaces rather than appends`() {
        var text = "سباحة"
        text = withTime(text, today.atTime(15, 0))
        text = withTime(text, today.atTime(9, 0))
        text = withDuration(text, 60)
        text = withDuration(text, 30)

        assertEquals("سباحة اليوم الساعة 9ص لمدة 30 دقيقة", text)
        assertEquals("سباحة", parse(text).title)
        assertEquals(30, parse(text).durationMinutes)
    }

    @Test
    fun `every time the writer can emit parses back to itself`() {
        for (dayOffset in 0..40L) {
            for (hour in 0..23) {
                val moment = today.plusDays(dayOffset).atTime(hour, if (hour % 2 == 0) 0 else 30)
                val sentence = withTime("مهمة", moment)

                assertEquals(sentence, moment, parse(sentence).startAt)
                assertEquals(sentence, "مهمة", parse(sentence).title)
            }
        }
    }

    @Test
    fun `every length the writer can emit parses back to itself`() {
        for (minutes in listOf(5, 15, 30, 45, 60, 75, 90, 120, 150, 180, 240, 480)) {
            val sentence = withDuration("مهمة", minutes)

            assertEquals(sentence, minutes, parse(sentence).durationMinutes)
            assertEquals(sentence, "مهمة", parse(sentence).title)
        }
    }

    @Test
    fun `a written category round-trips`() {
        val sentence = TaskInputWriter.withCategory("مهمة", parse("مهمة"), "Work")

        assertEquals("مهمة @Work", sentence)
        assertEquals("Work", parse(sentence).categoryToken)
        assertEquals("مهمة", parse(sentence).title)
    }

    @Test
    fun `a full sentence survives every attribute at once`() {
        var text = "تمرين"
        text = withTime(text, today.plusDays(1).atTime(18, 30))
        text = withDuration(text, 45)
        text = TaskInputWriter.withCategory(text, parse(text), "Play")

        val result = parse(text)
        assertEquals("تمرين", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 23, 18, 30), result.startAt)
        assertEquals(45, result.durationMinutes)
        assertEquals("Play", result.categoryToken)
    }
}
