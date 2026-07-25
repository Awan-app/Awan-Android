package com.awan.app.core.domain.task.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.util.Locale

/** The English cases of [TaskInputParserTest], in Arabic. Same clock, same expectations. */
class TaskInputParserArabicTest {

    /** Wednesday 2026-07-22, 10:00. */
    private val now = LocalDateTime.of(2026, 7, 22, 10, 0)
    private val arabic: Locale = Locale.forLanguageTag("ar")

    private fun parse(input: String) = TaskInputParser.parse(input, now, arabic)

    @Test
    fun `a bare title stays unscheduled`() {
        val result = parse("شراء البقالة")

        assertEquals("شراء البقالة", result.title)
        assertNull(result.startAt)
        assertNull(result.durationMinutes)
        assertTrue(result.tokens.isEmpty())
    }

    @Test
    fun `tomorrow plus a meridiem time`() {
        val result = parse("تمرين غدا 6م")

        assertEquals("تمرين", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 23, 18, 0), result.startAt)
    }

    @Test
    fun `a date without a time defaults to 9am`() {
        val result = parse("اتصل بالطبيب غدا")

        assertEquals("اتصل بالطبيب", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 23, 9, 0), result.startAt)
    }

    @Test
    fun `tonight defaults to the evening rather than 9am`() {
        val result = parse("اقرأ فصلا الليلة")

        assertEquals("اقرأ فصلا", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 22, 20, 0), result.startAt)
    }

    @Test
    fun `a weekday resolves to the next occurrence, and its qualifier is consumed`() {
        val result = parse("اجتماع الجمعة القادمة")

        assertEquals("اجتماع", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 24, 9, 0), result.startAt)
    }

    @Test
    fun `a weekday prefix is consumed too`() {
        val result = parse("اجتماع يوم الاثنين الساعة 3م")

        assertEquals("اجتماع", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 27, 15, 0), result.startAt)
    }

    @Test
    fun `a time range gives a start and a length`() {
        val result = parse("سباحة من 3م إلى 5م")

        assertEquals("سباحة", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 22, 15, 0), result.startAt)
        assertEquals(120, result.durationMinutes)
    }

    @Test
    fun `a duration is lifted out of the title`() {
        val result = parse("مراجعة لمدة 45 دقيقة")

        assertEquals("مراجعة", result.title)
        assertEquals(45, result.durationMinutes)
    }

    @Test
    fun `an hour duration reads as sixty minutes`() {
        assertEquals(60, parse("مراجعة لمدة 1 ساعة").durationMinutes)
        assertEquals(180, parse("مراجعة لمدة 3 ساعات").durationMinutes)
    }

    @Test
    fun `a relative offset resolves against now`() {
        val result = parse("اتصل بعد 20 دقيقة")

        assertEquals("اتصل", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 22, 10, 20), result.startAt)
    }

    @Test
    fun `in N hours is not read as a duration`() {
        val result = parse("اتصل بعد 2 ساعات")

        assertEquals(LocalDateTime.of(2026, 7, 22, 12, 0), result.startAt)
        assertNull(result.durationMinutes)
    }

    @Test
    fun `parts of this day resolve to their usual hour`() {
        assertEquals(LocalDateTime.of(2026, 7, 22, 9, 0), parse("اجري هذا الصباح").startAt)
        assertEquals(LocalDateTime.of(2026, 7, 22, 14, 0), parse("اجري بعد الظهر").startAt)
        assertEquals(LocalDateTime.of(2026, 7, 22, 19, 0), parse("اجري هذا المساء").startAt)
        assertEquals("اجري", parse("اجري هذا المساء").title)
    }

    @Test
    fun `an explicit clock time beats the hour a day part would default to`() {
        val result = parse("اتصل هذا الصباح الساعة 10")

        assertEquals("اتصل", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 22, 10, 0), result.startAt)
    }

    @Test
    fun `next week lands a week out`() {
        val result = parse("خطط للأسبوع الأسبوع القادم")

        assertEquals(LocalDateTime.of(2026, 7, 29, 9, 0), result.startAt)
    }

    @Test
    fun `noon and midnight are literal times`() {
        assertEquals(LocalDateTime.of(2026, 7, 22, 12, 0), parse("غداء الظهر").startAt)
        assertEquals(LocalDateTime.of(2026, 7, 23, 0, 0), parse("نسخة احتياطية منتصف الليل").startAt)
    }

    @Test
    fun `a category token is lifted out of the title`() {
        val result = parse("شراء البقالة @Work")

        assertEquals("شراء البقالة", result.title)
        assertEquals("Work", result.categoryToken)
    }

    @Test
    fun `arabic-indic digits read the same as ascii ones`() {
        val result = parse("تمرين غدا ٦م لمدة ٤٥ دقيقة")

        assertEquals("تمرين", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 23, 18, 0), result.startAt)
        assertEquals(45, result.durationMinutes)
    }

    @Test
    fun `a title that names no time survives whole`() {
        val result = parse("اشتري 3 كتب عن التاريخ")

        assertEquals("اشتري 3 كتب عن التاريخ", result.title)
        assertNull(result.startAt)
        assertNull(result.durationMinutes)
    }

    @Test
    fun `english keywords are not read as arabic ones`() {
        val result = parse("مهمة tomorrow")

        assertEquals("مهمة tomorrow", result.title)
        assertNull(result.startAt)
    }

    @Test
    fun `token ranges never overlap`() {
        val tokens = parse("تمرين غدا الساعة 6م لمدة 45 دقيقة @Work").tokens
        tokens.zipWithNext { left, right ->
            assertTrue("$left overlaps $right", left.range.last < right.range.first)
        }
    }
}
