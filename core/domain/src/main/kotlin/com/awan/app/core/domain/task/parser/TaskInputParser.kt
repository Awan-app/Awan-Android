package com.awan.app.core.domain.task.parser

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Lifts scheduling hints out of a typed sentence: `Go Swimming from 3pm to 5pm` becomes the title
 * `Go Swimming` plus a start time and a two-hour length. Whatever isn't recognised stays in the
 * title, so the parser can only ever be *less* helpful — never wrong about what the user meant to
 * write down.
 *
 * The typed sentence is the single source of truth for a draft. [TaskInputWriter] is the other half
 * of that contract: it writes attributes *back* into the sentence in a form this parser re-reads.
 *
 * Pure and deterministic: [parse] takes `now` rather than reading the clock.
 *
 * ponytail: English keywords only. The upgrade path is keying [WEEKDAYS] and the literal tables by
 * Locale and falling back to `DayOfWeek.getDisplayName(TextStyle.SHORT, locale)`.
 */
object TaskInputParser {

    const val DEFAULT_HOUR = 9

    private const val TONIGHT_HOUR = 20
    private const val MORNING_HOUR = 9
    private const val AFTERNOON_HOUR = 14
    private const val EVENING_HOUR = 19
    private const val NOON_HOUR = 12
    private const val MAX_HOUR = 23
    private const val MAX_MINUTE = 59
    private const val MINUTES_PER_HOUR = 60
    private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
    private const val HOURS_PER_HALF_DAY = 12
    private const val DAYS_PER_WEEK = 7L

    /** A bare hour up to this reads as afternoon: `at 3` is 3pm, the way people say it out loud. */
    private const val BARE_PM_MAX_HOUR = 7

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

    private const val CLOCK = """(\d{1,2})(?::(\d{2}))?\s*(am|pm)?"""
    private const val UNIT_HOURS = """h|hr|hrs|hour|hours"""
    private const val UNIT_MINUTES = """m|min|mins|minute|minutes"""

    private val CATEGORY = Regex("""@([\p{L}\p{N}_-]+)""")

    /** `from 3pm to 5pm`, `3pm-5pm`, `3-5pm`, `15:00 until 17:00`. */
    private val TIME_RANGE = Regex(
        """\b(?:from\s+)?$CLOCK\s*(?:-|–|—|to|until|till)\s*$CLOCK\b""",
        RegexOption.IGNORE_CASE,
    )
    private val RELATIVE_IN = Regex(
        """\bin\s+(\d+)\s*($UNIT_MINUTES|$UNIT_HOURS|d|day|days|w|week|weeks)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val DURATION = Regex(
        """\b(?:for\s+)?(?:(\d+)\s*(?:$UNIT_HOURS)(?:\s*(\d{1,2})\s*(?:$UNIT_MINUTES)?)?""" +
            """|(\d+)\s*(?:$UNIT_MINUTES))\b""",
        RegexOption.IGNORE_CASE,
    )
    private val TIME_MERIDIEM = Regex("""\b(?:at\s+)?(\d{1,2})(?::(\d{2}))?\s*(am|pm)\b""", RegexOption.IGNORE_CASE)
    /** The `at` is the whole guard: without it `Read chapter 3` would become a time. */
    private val TIME_AT = Regex("""\bat\s+(\d{1,2})(?::(\d{2}))?\b""", RegexOption.IGNORE_CASE)
    private val TIME_24H = Regex("""\b(?:at\s+)?(\d{1,2}):(\d{2})\b""")
    private val NOON = Regex("""\bnoon\b""", RegexOption.IGNORE_CASE)
    private val MIDNIGHT = Regex("""\bmidnight\b""", RegexOption.IGNORE_CASE)
    private val DAY_PART = Regex("""\bthis\s+(morning|afternoon|evening)\b""", RegexOption.IGNORE_CASE)
    private val RELATIVE_DAY = Regex("""\b(today|tonight|tomorrow)\b""", RegexOption.IGNORE_CASE)
    private val NEXT_WEEK = Regex("""\bnext\s+week\b""", RegexOption.IGNORE_CASE)
    private val WEEKDAY = Regex(
        """\b(?:next\s+|by\s+|on\s+)?(${WEEKDAYS.keys.joinToString("|")})\b""",
        RegexOption.IGNORE_CASE,
    )
    private val NUMERIC_DATE = Regex("""\b(\d{1,2})[/-](\d{1,2})\b""")
    private val WHITESPACE = Regex("""\s+""")

    fun parse(input: String, now: LocalDateTime): ParsedTaskInput {
        if (input.isBlank()) return ParsedTaskInput.Empty

        val claimed = mutableListOf<TaskToken>()
        // Every matcher runs so it consumes its phrase out of the title, even when a
        // higher-priority match already supplied the value.
        val category = matchCategory(input, claimed)
        val range = matchTimeRange(input, claimed)
        val relative = matchRelativeIn(input, claimed, now)
        val dayPart = matchDayPart(input, claimed, now.toLocalDate())
        val explicitDuration = matchDuration(input, claimed)
        val explicitTime = matchTime(input, claimed)
        val explicitDate = matchDate(input, claimed, now.toLocalDate())

        val anchored = relative ?: dayPart
        val duration = range?.durationMinutes ?: explicitDuration
        val time = range?.start ?: anchored?.toLocalTime() ?: explicitTime
        val date = anchored?.toLocalDate()?.let(::MatchedDate) ?: explicitDate

        return ParsedTaskInput(
            title = titleFrom(input, claimed),
            startAt = resolveStart(date, time, now),
            durationMinutes = duration,
            categoryToken = category,
            tokens = claimed.sortedBy { it.range.first },
            hasExplicitTime = time != null,
        )
    }

    // ── Token matching ───────────────────────────────────────────────────────

    private fun matchCategory(input: String, claimed: MutableList<TaskToken>): String? =
        CATEGORY.find(input)
            ?.also { claimed.claim(it.range, TaskTokenKind.CATEGORY) }
            ?.groupValues?.get(1)

    private data class MatchedRange(val start: LocalTime, val durationMinutes: Int)

    /**
     * Requires at least one side to carry `am`/`pm` or a colon, so `chapter 3 to 5` stays a title.
     * A bare left side borrows the right side's meridiem, which is what makes `3-5pm` work.
     */
    private fun matchTimeRange(input: String, claimed: MutableList<TaskToken>): MatchedRange? {
        val match = TIME_RANGE.findAll(input).firstOrNull { claimed.isFree(it.range) } ?: return null
        val g = match.groupValues
        val leftMeridiem = g[3].lowercase().ifEmpty { null }
        val rightMeridiem = g[6].lowercase().ifEmpty { null }
        val hasMinutes = g[2].isNotEmpty() || g[5].isNotEmpty()
        if (leftMeridiem == null && rightMeridiem == null && !hasMinutes) return null

        val start = clockTime(g[1], g[2], leftMeridiem ?: rightMeridiem) ?: return null
        val end = clockTime(g[4], g[5], rightMeridiem ?: leftMeridiem) ?: return null

        // 10pm to 1am is a real span that crosses midnight, not a mistake.
        val span = ((end.toSecondOfDay() - start.toSecondOfDay()) / 60).let {
            if (it <= 0) it + MINUTES_PER_DAY else it
        }
        claimed.add(
            TaskToken(
                range = match.range,
                kind = TaskTokenKind.DATE_TIME,
                // Tint the two clocks, leave `from` and `to` plain.
                highlights = listOfNotNull(match.clockHighlight(1), match.clockHighlight(4)),
            ),
        )
        return MatchedRange(start, span)
    }

    /** One clock spanning its hour, optional minutes and optional meridiem, as one piece. */
    private fun MatchResult.clockHighlight(hourGroup: Int): IntRange? {
        val first = groups[hourGroup]?.range?.first ?: return null
        val last = groups[hourGroup + 2]?.range?.last
            ?: groups[hourGroup + 1]?.range?.last
            ?: groups[hourGroup]?.range?.last
            ?: return null
        return first..last
    }

    private fun clockTime(hourText: String, minuteText: String, meridiem: String?): LocalTime? {
        val rawHour = hourText.toIntOrNull() ?: return null
        val minute = minuteText.toIntOrNull() ?: 0
        if (minute > MAX_MINUTE) return null
        val hour = when {
            meridiem == null -> rawHour
            rawHour !in 1..HOURS_PER_HALF_DAY -> return null
            meridiem == "pm" && rawHour < HOURS_PER_HALF_DAY -> rawHour + HOURS_PER_HALF_DAY
            meridiem == "am" && rawHour == HOURS_PER_HALF_DAY -> 0
            else -> rawHour
        }
        if (hour > MAX_HOUR) return null
        return LocalTime.of(hour, minute)
    }

    /** `in 20 minutes`, `in 2 hours`, `in 3 days` — resolved against `now`, so it carries a date too. */
    private fun matchRelativeIn(
        input: String,
        claimed: MutableList<TaskToken>,
        now: LocalDateTime,
    ): LocalDateTime? {
        val match = RELATIVE_IN.findAll(input).firstOrNull { claimed.isFree(it.range) } ?: return null
        val amount = match.groupValues[1].toLongOrNull()?.takeIf { it > 0 } ?: return null
        val unit = match.groupValues[2].lowercase()
        val moment = when {
            unit.startsWith("w") -> now.plusWeeks(amount)
            unit.startsWith("d") -> now.plusDays(amount)
            unit.startsWith("h") -> now.plusHours(amount)
            else -> now.plusMinutes(amount)
        }
        claimed.claim(match.range, TaskTokenKind.DATE_TIME)
        return moment
    }

    private fun matchDuration(input: String, claimed: MutableList<TaskToken>): Int? {
        val match = DURATION.findAll(input).firstOrNull { claimed.isFree(it.range) } ?: return null
        val hours = match.groupValues[1].toIntOrNull()
        val hourMinutes = match.groupValues[2].toIntOrNull()
        val plainMinutes = match.groupValues[3].toIntOrNull()
        val minutes = when {
            hours != null -> hours * MINUTES_PER_HOUR + (hourMinutes ?: 0)
            else -> plainMinutes ?: return null
        }
        if (minutes <= 0) return null
        claimed.claim(match.range, TaskTokenKind.DURATION)
        return minutes
    }

    /**
     * Meridiem first so `at 3pm` is never shifted twice, and the bare `at …` rule before the plain
     * 24-hour one so `at 3` and `at 3:30` cannot disagree about which half of the day they mean.
     */
    private fun matchTime(input: String, claimed: MutableList<TaskToken>): LocalTime? =
        matchMeridiemTime(input, claimed)
            ?: matchBareClockTime(input, claimed)
            ?: match24HourTime(input, claimed)
            ?: matchLiteralTime(input, claimed)

    private fun matchMeridiemTime(input: String, claimed: MutableList<TaskToken>): LocalTime? {
        val match = TIME_MERIDIEM.findAll(input).firstOrNull { claimed.isFree(it.range) } ?: return null
        val time = clockTime(match.groupValues[1], match.groupValues[2], match.groupValues[3].lowercase())
            ?: return null
        claimed.claim(match.range, TaskTokenKind.DATE_TIME)
        return time
    }

    /**
     * `at 3`, `at 3:30`, `at 20`. Saying `at` is the user committing the number to being a clock,
     * so the only thing left to guess is which half of the day — see [BARE_PM_MAX_HOUR].
     */
    private fun matchBareClockTime(input: String, claimed: MutableList<TaskToken>): LocalTime? {
        val match = TIME_AT.findAll(input).firstOrNull { claimed.isFree(it.range) } ?: return null
        val time = clockTime(match.groupValues[1], match.groupValues[2], meridiem = null) ?: return null
        claimed.claim(match.range, TaskTokenKind.DATE_TIME)
        return if (time.hour in 1..BARE_PM_MAX_HOUR) time.plusHours(HOURS_PER_HALF_DAY.toLong()) else time
    }

    private fun match24HourTime(input: String, claimed: MutableList<TaskToken>): LocalTime? {
        val match = TIME_24H.findAll(input).firstOrNull { claimed.isFree(it.range) } ?: return null
        val time = clockTime(match.groupValues[1], match.groupValues[2], meridiem = null) ?: return null
        claimed.claim(match.range, TaskTokenKind.DATE_TIME)
        return time
    }

    /**
     * `this morning` names a day as well as an hour, so it returns both — otherwise the
     * time-only rule would roll a morning task to tomorrow whenever it was typed after 9am.
     */
    private fun matchDayPart(
        input: String,
        claimed: MutableList<TaskToken>,
        today: LocalDate,
    ): LocalDateTime? {
        val match = DAY_PART.find(input)?.takeIf { claimed.isFree(it.range) } ?: return null
        claimed.claim(match.range, TaskTokenKind.DATE_TIME)
        val hour = when (match.groupValues[1].lowercase()) {
            "morning" -> MORNING_HOUR
            "afternoon" -> AFTERNOON_HOUR
            else -> EVENING_HOUR
        }
        return today.atTime(hour, 0)
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
            ?: matchNextWeek(input, claimed, today)
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

    private fun matchNextWeek(input: String, claimed: MutableList<TaskToken>, today: LocalDate): MatchedDate? {
        val match = NEXT_WEEK.find(input)?.takeIf { claimed.isFree(it.range) } ?: return null
        claimed.claim(match.range, TaskTokenKind.DATE_TIME)
        return MatchedDate(today.plusWeeks(1))
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
