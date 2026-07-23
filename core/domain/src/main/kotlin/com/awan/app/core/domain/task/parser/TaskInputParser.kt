package com.awan.app.core.domain.task.parser

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Lifts scheduling hints out of a typed sentence: `Gym session tomorrow 6pm @play` becomes the
 * title `Gym session` plus a start time and a zone token. Whatever isn't recognised stays in the
 * title, so the parser can only ever be *less* helpful — never wrong about what the user meant to
 * write down.
 *
 * Pure and deterministic: [parse] takes `now` rather than reading the clock.
 *
 * ponytail: English keywords only. The upgrade path is keying [WEEKDAYS] and the literal tables by
 * Locale and falling back to `DayOfWeek.getDisplayName(TextStyle.SHORT, locale)`.
 */
object TaskInputParser {

    private const val DEFAULT_HOUR = 9
    private const val TONIGHT_HOUR = 20
    private const val NOON_HOUR = 12
    private const val MAX_HOUR = 23
    private const val MAX_MINUTE = 59
    private const val MINUTES_PER_HOUR = 60
    private const val HOURS_PER_HALF_DAY = 12
    private const val DAYS_PER_WEEK = 7L

    private val WEEKDAYS: Map<String, DayOfWeek> = mapOf(
        "mon" to DayOfWeek.MONDAY, "monday" to DayOfWeek.MONDAY,
        "tue" to DayOfWeek.TUESDAY, "tues" to DayOfWeek.TUESDAY, "tuesday" to DayOfWeek.TUESDAY,
        "wed" to DayOfWeek.WEDNESDAY, "weds" to DayOfWeek.WEDNESDAY, "wednesday" to DayOfWeek.WEDNESDAY,
        "thu" to DayOfWeek.THURSDAY, "thur" to DayOfWeek.THURSDAY, "thurs" to DayOfWeek.THURSDAY,
        "thursday" to DayOfWeek.THURSDAY,
        "fri" to DayOfWeek.FRIDAY, "friday" to DayOfWeek.FRIDAY,
        "sat" to DayOfWeek.SATURDAY, "saturday" to DayOfWeek.SATURDAY,
        "sun" to DayOfWeek.SUNDAY, "sunday" to DayOfWeek.SUNDAY,
    )

    private val ZONE = Regex("""@([\p{L}\p{N}_-]+)""")
    private val DURATION = Regex(
        """\b(?:for\s+)?(?:(\d+)\s*(?:h|hr|hrs|hour|hours)(?:\s*(\d{1,2})\s*(?:m|min|mins|minute|minutes)?)?""" +
            """|(\d+)\s*(?:m|min|mins|minute|minutes))\b""",
        RegexOption.IGNORE_CASE,
    )
    private val TIME_MERIDIEM = Regex("""\b(?:at\s+)?(\d{1,2})(?::(\d{2}))?\s*(am|pm)\b""", RegexOption.IGNORE_CASE)
    private val TIME_24H = Regex("""\b(?:at\s+)?(\d{1,2}):(\d{2})\b""")
    private val NOON = Regex("""\bnoon\b""", RegexOption.IGNORE_CASE)
    private val MIDNIGHT = Regex("""\bmidnight\b""", RegexOption.IGNORE_CASE)
    private val RELATIVE_DAY = Regex("""\b(today|tonight|tomorrow)\b""", RegexOption.IGNORE_CASE)
    private val WEEKDAY = Regex(
        """\b(?:next\s+|by\s+|on\s+)?(${WEEKDAYS.keys.joinToString("|")})\b""",
        RegexOption.IGNORE_CASE,
    )
    private val NUMERIC_DATE = Regex("""\b(\d{1,2})[/-](\d{1,2})\b""")
    private val WHITESPACE = Regex("""\s+""")

    fun parse(input: String, now: LocalDateTime): ParsedTaskInput {
        if (input.isBlank()) return ParsedTaskInput.Empty

        val claimed = mutableListOf<TaskToken>()
        val zone = matchZone(input, claimed)
        val duration = matchDuration(input, claimed)
        val time = matchTime(input, claimed)
        val date = matchDate(input, claimed, now.toLocalDate())

        return ParsedTaskInput(
            title = titleFrom(input, claimed),
            startAt = resolveStart(date, time, now),
            durationMinutes = duration,
            zoneToken = zone,
            tokens = claimed.sortedBy { it.range.first },
        )
    }

    // ── Token matching ───────────────────────────────────────────────────────

    private fun matchZone(input: String, claimed: MutableList<TaskToken>): String? =
        ZONE.find(input)
            ?.also { claimed.claim(it.range, TaskTokenKind.ZONE) }
            ?.groupValues?.get(1)

    private fun matchDuration(input: String, claimed: MutableList<TaskToken>): Int? {
        val match = DURATION.findAll(input).firstOrNull { claimed.isFree(it.range) } ?: return null
        val (hours, hourMinutes, plainMinutes) = match.destructureGroups()
        val minutes = when {
            hours != null -> hours * MINUTES_PER_HOUR + (hourMinutes ?: 0)
            else -> plainMinutes ?: return null
        }
        if (minutes <= 0) return null
        claimed.claim(match.range, TaskTokenKind.DURATION)
        return minutes
    }

    private fun MatchResult.destructureGroups(): Triple<Int?, Int?, Int?> = Triple(
        groupValues[1].toIntOrNull(),
        groupValues[2].toIntOrNull(),
        groupValues[3].toIntOrNull(),
    )

    private fun matchTime(input: String, claimed: MutableList<TaskToken>): LocalTime? =
        matchMeridiemTime(input, claimed)
            ?: match24HourTime(input, claimed)
            ?: matchLiteralTime(input, claimed)

    private fun matchMeridiemTime(input: String, claimed: MutableList<TaskToken>): LocalTime? {
        val match = TIME_MERIDIEM.findAll(input).firstOrNull { claimed.isFree(it.range) } ?: return null
        val rawHour = match.groupValues[1].toIntOrNull() ?: return null
        if (rawHour !in 1..HOURS_PER_HALF_DAY) return null
        val minute = match.groupValues[2].toIntOrNull() ?: 0
        if (minute > MAX_MINUTE) return null
        val isPm = match.groupValues[3].lowercase() == "pm"
        val hour = when {
            isPm && rawHour < HOURS_PER_HALF_DAY -> rawHour + HOURS_PER_HALF_DAY
            !isPm && rawHour == HOURS_PER_HALF_DAY -> 0
            else -> rawHour
        }
        claimed.claim(match.range, TaskTokenKind.DATE_TIME)
        return LocalTime.of(hour, minute)
    }

    private fun match24HourTime(input: String, claimed: MutableList<TaskToken>): LocalTime? {
        val match = TIME_24H.findAll(input).firstOrNull { claimed.isFree(it.range) } ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        if (hour > MAX_HOUR || minute > MAX_MINUTE) return null
        claimed.claim(match.range, TaskTokenKind.DATE_TIME)
        return LocalTime.of(hour, minute)
    }

    private fun matchLiteralTime(input: String, claimed: MutableList<TaskToken>): LocalTime? {
        NOON.find(input)?.takeIf { claimed.isFree(it.range) }?.let {
            claimed.claim(it.range, TaskTokenKind.DATE_TIME)
            return LocalTime.of(NOON_HOUR, 0)
        }
        MIDNIGHT.find(input)?.takeIf { claimed.isFree(it.range) }?.let {
            claimed.claim(it.range, TaskTokenKind.DATE_TIME)
            return LocalTime.MIDNIGHT
        }
        return null
    }

    private fun matchDate(input: String, claimed: MutableList<TaskToken>, today: LocalDate): MatchedDate? =
        matchRelativeDay(input, claimed, today)
            ?: matchWeekday(input, claimed, today)
            ?: matchNumericDate(input, claimed, today)

    private fun matchRelativeDay(input: String, claimed: MutableList<TaskToken>, today: LocalDate): MatchedDate? {
        val match = RELATIVE_DAY.findAll(input).firstOrNull { claimed.isFree(it.range) } ?: return null
        claimed.claim(match.range, TaskTokenKind.DATE_TIME)
        return when (match.groupValues[1].lowercase()) {
            "tomorrow" -> MatchedDate(today.plusDays(1))
            "tonight" -> MatchedDate(today, defaultHour = TONIGHT_HOUR)
            else -> MatchedDate(today)
        }
    }

    /** `next fri`, `by fri` and a bare `fri` all mean the next Friday that isn't today. */
    private fun matchWeekday(input: String, claimed: MutableList<TaskToken>, today: LocalDate): MatchedDate? {
        val match = WEEKDAY.findAll(input).firstOrNull { claimed.isFree(it.range) } ?: return null
        val target = WEEKDAYS[match.groupValues[1].lowercase()] ?: return null
        claimed.claim(match.range, TaskTokenKind.DATE_TIME)
        val delta = (target.value - today.dayOfWeek.value + DAYS_PER_WEEK) % DAYS_PER_WEEK
        return MatchedDate(today.plusDays(if (delta == 0L) DAYS_PER_WEEK else delta))
    }

    private fun matchNumericDate(input: String, claimed: MutableList<TaskToken>, today: LocalDate): MatchedDate? {
        val match = NUMERIC_DATE.findAll(input).firstOrNull { claimed.isFree(it.range) } ?: return null
        val day = match.groupValues[1].toIntOrNull() ?: return null
        val month = match.groupValues[2].toIntOrNull() ?: return null
        val date = runCatching { LocalDate.of(today.year, month, day) }.getOrNull() ?: return null
        claimed.claim(match.range, TaskTokenKind.DATE_TIME)
        return MatchedDate(if (date.isBefore(today)) date.plusYears(1) else date)
    }

    // ── Assembly ─────────────────────────────────────────────────────────────

    private fun titleFrom(input: String, claimed: List<TaskToken>): String {
        val kept = StringBuilder(input.length)
        input.forEachIndexed { index, char ->
            if (claimed.none { index in it.range }) kept.append(char)
        }
        return kept.toString().replace(WHITESPACE, " ").trim()
    }

    private fun resolveStart(date: MatchedDate?, time: LocalTime?, now: LocalDateTime): LocalDateTime? = when {
        date != null && time != null -> date.value.atTime(time)
        date != null -> date.value.atTime(date.defaultHour, 0)
        time != null -> now.toLocalDate().atTime(time).let { if (it.isAfter(now)) it else it.plusDays(1) }
        else -> null
    }

    private data class MatchedDate(val value: LocalDate, val defaultHour: Int = DEFAULT_HOUR)

    private fun MutableList<TaskToken>.claim(range: IntRange, kind: TaskTokenKind) {
        add(TaskToken(range, kind))
    }

    private fun List<TaskToken>.isFree(range: IntRange): Boolean =
        none { it.range.first <= range.last && range.first <= it.range.last }
}
