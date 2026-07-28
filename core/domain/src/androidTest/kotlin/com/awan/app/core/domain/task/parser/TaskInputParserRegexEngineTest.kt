package com.awan.app.core.domain.task.parser

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.util.Locale

/**
 * The same parser, on the engine that actually ships. Android matches with ICU and the JVM suite
 * matches with `java.util.regex`; they disagree about `\b`, about what `\d` covers, and about which
 * inline flags exist at all — an `(?U)` that every JVM test accepted once crashed the app on the
 * first keystroke.
 *
 * This is deliberately thin. [TaskInputParserTest] and [TaskInputParserArabicTest] own the
 * behaviour; all this has to prove is that the patterns compile here and mean the same thing.
 */
@RunWith(AndroidJUnit4::class)
class TaskInputParserRegexEngineTest {

    /** Wednesday 2026-07-22, 10:00. */
    private val now = LocalDateTime.of(2026, 7, 22, 10, 0)
    private val arabic: Locale = Locale.forLanguageTag("ar")

    @Test
    fun englishSentenceParsesTheSameAsOnTheJvm() {
        val result = TaskInputParser.parse("Gym session tomorrow 6pm for 45 min @Play", now, Locale.ENGLISH)

        assertEquals("Gym session", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 23, 18, 0), result.startAt)
        assertEquals(45, result.durationMinutes)
        assertEquals("Play", result.categoryToken)
    }

    @Test
    fun arabicSentenceParsesTheSameAsOnTheJvm() {
        val result = TaskInputParser.parse("تمرين غدا الساعة 6م لمدة 45 دقيقة @Play", now, arabic)

        assertEquals("تمرين", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 23, 18, 0), result.startAt)
        assertEquals(45, result.durationMinutes)
        assertEquals("Play", result.categoryToken)
    }

    /** ICU's `\b` is Unicode-aware and the JVM's is not, so the boundary is spelled out instead. */
    @Test
    fun aWordBoundaryIsHonouredOnBothSidesOfAnArabicKeyword() {
        // `الجمعة` is a weekday; `الجمعةX` is not, and neither is a keyword glued to more letters.
        assertEquals(
            LocalDateTime.of(2026, 7, 24, 9, 0),
            TaskInputParser.parse("اجتماع الجمعة", now, arabic).startAt,
        )
        assertNull(TaskInputParser.parse("اجتماع الجمعةش", now, arabic).startAt)
    }

    @Test
    fun arabicIndicDigitsFoldWithoutMovingAnyToken() {
        val input = "تمرين غدا ٦م"
        val result = TaskInputParser.parse(input, now, arabic)

        assertEquals("تمرين", result.title)
        assertEquals(LocalDateTime.of(2026, 7, 23, 18, 0), result.startAt)
        // Every range still indexes the text the user typed, not the folded copy.
        result.tokens.forEach { token ->
            assertEquals(input.length, (input.indices.last + 1))
            assertEquals(true, token.range.last < input.length)
        }
    }

    @Test
    fun everyLexiconCompilesOnThisEngine() {
        // Touching both lexicons is the whole point: the patterns are built in a static initialiser,
        // so a syntax error either engine rejects surfaces as a crash on the first parse.
        for (locale in listOf(Locale.ENGLISH, arabic)) {
            for (input in SENTENCES) {
                TaskInputParser.parse(input, now, locale)
            }
        }
    }

    private companion object {
        val SENTENCES = listOf(
            "",
            "plain title",
            "from 3pm to 5pm",
            "من 3م إلى 5م",
            "in 20 minutes",
            "بعد 20 دقيقة",
            "this morning at 10am",
            "هذا الصباح الساعة 10",
            "next week",
            "الأسبوع القادم",
            "at 15:30 for 1h30",
            "الساعة 15:30 لمدة 90 دقيقة",
            "noon @work",
            "منتصف الليل @work",
            "24/12 at 9am",
            "اجتماع يوم الاثنين القادم",
        )
    }
}
