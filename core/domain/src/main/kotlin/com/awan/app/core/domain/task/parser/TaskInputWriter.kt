package com.awan.app.core.domain.task.parser

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * The other half of [TaskInputParser]. When a chip is used to pick a time or a length, the choice is
 * written back into the typed sentence rather than held beside it — so the sentence stays the single
 * source of truth and the chips stay a pure readout of it.
 *
 * Every phrase produced here is one [TaskInputParser] re-reads to the same value, which is why the
 * words are English even when the UI is not: they are parser syntax, not display copy.
 */
object TaskInputWriter {

    private const val MINUTES_PER_HOUR = 60
    private const val HOURS_PER_HALF_DAY = 12
    private const val NEAR_DAYS = 6L

    /** Replaces whatever date/time phrases the sentence already had, or appends one if it had none. */
    fun withTime(input: String, parsed: ParsedTaskInput, moment: LocalDateTime, today: LocalDate): String =
        input.replacing(parsed.tokensOf(TaskTokenKind.DATE_TIME), timePhrase(moment, today))

    /**
     * Moves the day and keeps whatever clock time the sentence already stated. A date/time is spread
     * across several tokens, so replacing them all with a bare day would quietly drop a time that was
     * already there — `Gym tomorrow at 6pm` moved to Friday is `Gym friday at 6pm`, not `Gym friday`.
     */
    fun withDate(input: String, parsed: ParsedTaskInput, date: LocalDate, today: LocalDate): String {
        val time = parsed.startAt?.toLocalTime()?.takeIf { parsed.hasExplicitTime }
        return when (time) {
            null -> input.replacing(parsed.tokensOf(TaskTokenKind.DATE_TIME), datePhrase(date, today))
            else -> withTime(input, parsed, date.atTime(time), today)
        }
    }

    /** Replaces whatever length phrase the sentence already had, or appends one if it had none. */
    fun withDuration(input: String, parsed: ParsedTaskInput, minutes: Int): String =
        input.replacing(parsed.tokensOf(TaskTokenKind.DURATION), durationPhrase(minutes))

    /** `today at 3pm`, `tomorrow at 3:30pm`, `friday at 9am`, `24/12 at 9am`. */
    fun timePhrase(moment: LocalDateTime, today: LocalDate): String =
        "${datePhrase(moment.toLocalDate(), today)} at ${clockPhrase(moment.toLocalTime())}"

    /** `for 45 min`, `for 2 hours`, `for 1h30`. */
    fun durationPhrase(minutes: Int): String {
        val hours = minutes / MINUTES_PER_HOUR
        val remainder = minutes % MINUTES_PER_HOUR
        return when {
            hours == 0 -> "for $remainder min"
            remainder == 0 && hours == 1 -> "for 1 hour"
            remainder == 0 -> "for $hours hours"
            else -> "for ${hours}h$remainder"
        }
    }

    private fun datePhrase(date: LocalDate, today: LocalDate): String {
        val delta = ChronoUnit.DAYS.between(today, date)
        return when {
            delta == 0L -> "today"
            delta == 1L -> "tomorrow"
            delta in 2..NEAR_DAYS -> date.dayOfWeek.name.lowercase()
            else -> "${date.dayOfMonth}/${date.monthValue}"
        }
    }

    private fun clockPhrase(time: LocalTime): String {
        val meridiem = if (time.hour < HOURS_PER_HALF_DAY) "am" else "pm"
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
