package com.awan.app.core.domain.task.parser

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * The other half of [TaskInputParser]. When a chip is used to pick a time or a length, the choice is
 * written back into the typed sentence rather than held beside it — so the sentence stays the single
 * source of truth and the chips stay a pure readout of it.
 *
 * The sentence is what the user reads, so it is written in their language. Both halves take their
 * words from the same [TaskLexicon], which is what keeps the contract true: every phrase produced
 * here is one [TaskInputParser] reads back — in that language — to the same value.
 */
object TaskInputWriter {

    private const val MINUTES_PER_HOUR = 60
    private const val HOURS_PER_HALF_DAY = 12
    private const val NEAR_DAYS = 6L

    /** Replaces whatever date/time phrases the sentence already had, or appends one if it had none. */
    fun withTime(
        input: String,
        parsed: ParsedTaskInput,
        moment: LocalDateTime,
        today: LocalDate,
        locale: Locale = Locale.getDefault(),
    ): String = input.replacing(parsed.tokensOf(TaskTokenKind.DATE_TIME), timePhrase(moment, today, locale))

    /**
     * Moves the day and keeps whatever clock time the sentence already stated. A date/time is spread
     * across several tokens, so replacing them all with a bare day would quietly drop a time that was
     * already there — `Gym tomorrow at 6pm` moved to Friday is `Gym friday at 6pm`, not `Gym friday`.
     */
    fun withDate(
        input: String,
        parsed: ParsedTaskInput,
        date: LocalDate,
        today: LocalDate,
        locale: Locale = Locale.getDefault(),
    ): String {
        val time = parsed.startAt?.toLocalTime()?.takeIf { parsed.hasExplicitTime }
        return when (time) {
            null -> input.replacing(
                parsed.tokensOf(TaskTokenKind.DATE_TIME),
                datePhrase(date, today, TaskLexicon.of(locale)),
            )

            else -> withTime(input, parsed, date.atTime(time), today, locale)
        }
    }

    /** Replaces whatever length phrase the sentence already had, or appends one if it had none. */
    fun withDuration(
        input: String,
        parsed: ParsedTaskInput,
        minutes: Int,
        locale: Locale = Locale.getDefault(),
    ): String = input.replacing(parsed.tokensOf(TaskTokenKind.DURATION), durationPhrase(minutes, locale))

    /** Replaces whatever `@category` the sentence already had, or appends one if it had none. */
    fun withCategory(input: String, parsed: ParsedTaskInput, categoryName: String): String =
        input.replacing(parsed.tokensOf(TaskTokenKind.CATEGORY), categoryPhrase(categoryName))

    /** `today at 3pm`, `tomorrow at 3:30pm`, `friday at 9am`, `24/12 at 9am`. */
    fun timePhrase(moment: LocalDateTime, today: LocalDate, locale: Locale = Locale.getDefault()): String {
        val lexicon = TaskLexicon.of(locale)
        return "${datePhrase(moment.toLocalDate(), today, lexicon)} " +
            "${lexicon.writeAt} ${clockPhrase(moment.toLocalTime(), lexicon)}"
    }

    /** `for 45 min`, `for 2 hours`, `for 1h30`. */
    fun durationPhrase(minutes: Int, locale: Locale = Locale.getDefault()): String {
        val lexicon = TaskLexicon.of(locale)
        val hours = minutes / MINUTES_PER_HOUR
        val remainder = minutes % MINUTES_PER_HOUR
        // The numeral stays even for a single hour: the parser only reads a unit that follows a digit.
        val length = when {
            hours == 0 -> "$remainder ${lexicon.writeMinutes}"
            remainder == 0 && hours == 1 -> "$hours ${lexicon.writeHour}"
            remainder == 0 -> "$hours ${lexicon.writeHours}"
            // No `1h30` shorthand in this language, so say the whole thing in minutes instead.
            lexicon.hourMinuteSeparator == null -> "$minutes ${lexicon.writeMinutes}"
            else -> "$hours${lexicon.hourMinuteSeparator}$remainder"
        }
        return "${lexicon.writeFor} $length"
    }

    /**
     * ponytail: first word only. The `@token` the parser reads cannot hold a space, so a multi-word
     * category round-trips by prefix — exact for the single-word defaults (Study/Work/Play/Personal),
     * and the caller resolves the rest by `startsWith`. Upgrade path: quote multi-word categories
     * (`@"Deep Work"`) on both sides.
     */
    private fun categoryPhrase(categoryName: String): String = "@${categoryName.substringBefore(' ')}"

    private fun datePhrase(date: LocalDate, today: LocalDate, lexicon: TaskLexicon): String {
        val delta = ChronoUnit.DAYS.between(today, date)
        return when {
            delta == 0L -> lexicon.writeToday
            delta == 1L -> lexicon.writeTomorrow
            delta in 2..NEAR_DAYS -> lexicon.writeWeekday.getValue(date.dayOfWeek)
            else -> "${date.dayOfMonth}/${date.monthValue}"
        }
    }

    private fun clockPhrase(time: LocalTime, lexicon: TaskLexicon): String {
        val meridiem = if (time.hour < HOURS_PER_HALF_DAY) lexicon.writeAm else lexicon.writePm
        val hour = when (val h = time.hour % HOURS_PER_HALF_DAY) {
            0 -> HOURS_PER_HALF_DAY
            else -> h
        }
        val minutes = if (time.minute == 0) "" else ":%02d".format(time.minute)
        return "$hour$minutes$meridiem"
    }

    /**
     * Drops every one of [tokens] and puts [phrase] where the first of them started, or appends it
     * when there were none. Clearing all of them is what stops repeated edits from piling phrases
     * up — `today at 3pm` is two tokens, and replacing only one leaves the other stranded.
     */
    private fun String.replacing(tokens: List<TaskToken>, phrase: String): String {
        if (tokens.isEmpty()) {
            return if (isBlank()) phrase else "${trimEnd()} $phrase"
        }
        val insertAt = tokens.minOf { it.range.first }
        val kept = StringBuilder(length + phrase.length)
        forEachIndexed { index, char ->
            if (index == insertAt) kept.append(' ').append(phrase).append(' ')
            if (tokens.none { index in it.range }) kept.append(char)
        }
        return kept.toString().replace(COLLAPSE, " ").trim()
    }

    private val COLLAPSE = Regex("""\s+""")
}
