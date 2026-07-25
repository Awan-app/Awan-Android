package com.awan.app.core.domain.task.parser

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale
import java.util.regex.Pattern

/**
 * Lifts scheduling hints out of a typed sentence: `Go Swimming from 3pm to 5pm` becomes the title
 * `Go Swimming` plus a start time and a two-hour length. Whatever isn't recognised stays in the
 * title, so the parser can only ever be *less* helpful — never wrong about what the user meant to
 * write down.
 *
 * The typed sentence is the single source of truth for a draft. [TaskInputWriter] is the other half
 * of that contract: it writes attributes *back* into the sentence in a form this parser re-reads.
 *
 * Pure and deterministic: [parse] takes `now` rather than reading the clock, and the language it
 * reads from [TaskLexicon] rather than the ambient locale.
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

    /**
     * `\b` is not usable here. Unit tests run on the JVM, where it is ASCII-only and so never fires
     * on Arabic, and the app runs on Android's ICU engine, where it is Unicode-aware — and which
     * rejects the `(?U)` flag that would have reconciled them. Spelling the boundary out keeps the
     * two engines in agreement, which is the only reason a JVM test says anything about the app.
     */
    private const val WORD = """[\p{L}\p{N}_]"""
    private const val NOT_AFTER_WORD = """(?<!$WORD)"""
    private const val NOT_BEFORE_WORD = """(?!$WORD)"""

    private val CATEGORY = Regex("""@([\p{L}\p{N}_-]+)""")
    private val NUMERIC_DATE = Regex("""$NOT_AFTER_WORD(\d{1,2})[/-](\d{1,2})$NOT_BEFORE_WORD""")
    private val WHITESPACE = Regex("""\s+""")

    private val english = Patterns(TaskLexicon.English)
    private val arabic = Patterns(TaskLexicon.Arabic)

    fun parse(input: String, now: LocalDateTime, locale: Locale = Locale.getDefault()): ParsedTaskInput {
        if (input.isBlank()) return ParsedTaskInput.Empty
        val lexicon = TaskLexicon.of(locale)
        val patterns = if (lexicon === TaskLexicon.Arabic) arabic else english
        // Digits are matched in ASCII, but every range still indexes the text the user typed: the
        // mapping is one character to one character, so it cannot move anything.
        val scan = input.toAsciiDigits()

        val claimed = mutableListOf<TaskToken>()
        // Every matcher runs so it consumes its phrase out of the title, even when a
        // higher-priority match already supplied the value.
        // The category reads the raw text: it is a name to match against the user's own, and folding
        // its digits would stop it matching.
        val category = matchCategory(input, claimed)
        val range = patterns.matchTimeRange(scan, claimed)
        val relative = patterns.matchRelativeIn(scan, claimed, now)
        val dayPart = patterns.matchDayPart(scan, claimed, now.toLocalDate())
        val explicitDuration = patterns.matchDuration(scan, claimed)
        val explicitTime = patterns.matchTime(scan, claimed)
        val explicitDate = patterns.matchDate(scan, claimed, now.toLocalDate())

        val anchored = relative ?: dayPart
        val duration = range?.durationMinutes ?: explicitDuration
        // An anchor phrase names an hour only as a default, so a stated clock beats it: `this
        // morning at 10am` keeps the morning's *day* and the clock's *time*.
        val time = range?.start ?: explicitTime ?: anchored?.toLocalTime()
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

    /**
     * Arabic-Indic digits are folded to ASCII before matching so `٣م` reads the same as `3pm`. The
     * two ranges map one code point to one code point, which is what lets every [TaskToken] keep
     * indexing the original string.
     */
    private fun String.toAsciiDigits(): String {
        if (none { it in '٠'..'٩' || it in '۰'..'۹' }) return this
        return map {
            when (it) {
                in '٠'..'٩' -> '0' + (it - '٠')
                in '۰'..'۹' -> '0' + (it - '۰')
                else -> it
            }
        }.joinToString("")
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
    private fun Patterns.matchTimeRange(input: String, claimed: MutableList<TaskToken>): MatchedRange? {
        val match = timeRange.findAll(input).firstOrNull { claimed.isFree(it.range) } ?: return null
        val g = match.groupValues
        val leftMeridiem = lexicon.meridiemOf(g[3])
        val rightMeridiem = lexicon.meridiemOf(g[6])
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

    private fun clockTime(hourText: String, minuteText: String, meridiem: Meridiem?): LocalTime? {
        val rawHour = hourText.toIntOrNull() ?: return null
        val minute = minuteText.toIntOrNull() ?: 0
        if (minute > MAX_MINUTE) return null
        val hour = when {
            meridiem == null -> rawHour
            rawHour !in 1..HOURS_PER_HALF_DAY -> return null
            meridiem == Meridiem.PM && rawHour < HOURS_PER_HALF_DAY -> rawHour + HOURS_PER_HALF_DAY
            meridiem == Meridiem.AM && rawHour == HOURS_PER_HALF_DAY -> 0
            else -> rawHour
        }
        if (hour > MAX_HOUR) return null
        return LocalTime.of(hour, minute)
    }

    /** `in 20 minutes`, `in 2 hours`, `in 3 days` — resolved against `now`, so it carries a date too. */
    private fun Patterns.matchRelativeIn(
        input: String,
        claimed: MutableList<TaskToken>,
        now: LocalDateTime,
    ): LocalDateTime? {
        val match = relativeIn.findAll(input).firstOrNull { claimed.isFree(it.range) } ?: return null
        val amount = match.groupValues[1].toLongOrNull()?.takeIf { it > 0 } ?: return null
        val moment = when (lexicon.unitOf(match.groupValues[2]) ?: return null) {
            TimeUnit.WEEK -> now.plusWeeks(amount)
            TimeUnit.DAY -> now.plusDays(amount)
            TimeUnit.HOUR -> now.plusHours(amount)
            TimeUnit.MINUTE -> now.plusMinutes(amount)
        }
        claimed.claim(match.range, TaskTokenKind.DATE_TIME)
        return moment
    }

    private fun Patterns.matchDuration(input: String, claimed: MutableList<TaskToken>): Int? {
        val match = duration.findAll(input).firstOrNull { claimed.isFree(it.range) } ?: return null
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
    private fun Patterns.matchTime(input: String, claimed: MutableList<TaskToken>): LocalTime? =
        matchMeridiemTime(input, claimed)
            ?: matchBareClockTime(input, claimed)
            ?: match24HourTime(input, claimed)
            ?: matchLiteralTime(input, claimed)

    private fun Patterns.matchMeridiemTime(input: String, claimed: MutableList<TaskToken>): LocalTime? {
        val match = timeMeridiem.findAll(input).firstOrNull { claimed.isFree(it.range) } ?: return null
        val time = clockTime(match.groupValues[1], match.groupValues[2], lexicon.meridiemOf(match.groupValues[3]))
            ?: return null
        claimed.claim(match.range, TaskTokenKind.DATE_TIME)
        return time
    }

    /**
     * `at 3`, `at 3:30`, `at 20`. Saying `at` is the user committing the number to being a clock,
     * so the only thing left to guess is which half of the day — see [BARE_PM_MAX_HOUR].
     */
    private fun Patterns.matchBareClockTime(input: String, claimed: MutableList<TaskToken>): LocalTime? {
        val match = timeAt.findAll(input).firstOrNull { claimed.isFree(it.range) } ?: return null
        val time = clockTime(match.groupValues[1], match.groupValues[2], meridiem = null) ?: return null
        claimed.claim(match.range, TaskTokenKind.DATE_TIME)
        return if (time.hour in 1..BARE_PM_MAX_HOUR) time.plusHours(HOURS_PER_HALF_DAY.toLong()) else time
    }

    private fun Patterns.match24HourTime(input: String, claimed: MutableList<TaskToken>): LocalTime? {
        val match = time24H.findAll(input).firstOrNull { claimed.isFree(it.range) } ?: return null
        val time = clockTime(match.groupValues[1], match.groupValues[2], meridiem = null) ?: return null
        claimed.claim(match.range, TaskTokenKind.DATE_TIME)
        return time
    }

    /**
     * `this morning` names a day as well as an hour, so it returns both — otherwise the
     * time-only rule would roll a morning task to tomorrow whenever it was typed after 9am.
     */
    private fun Patterns.matchDayPart(
        input: String,
        claimed: MutableList<TaskToken>,
        today: LocalDate,
    ): LocalDateTime? {
        val match = dayPart.find(input)?.takeIf { claimed.isFree(it.range) } ?: return null
        claimed.claim(match.range, TaskTokenKind.DATE_TIME)
        val phrase = match.groupValues[1].collapsed()
        val hour = when {
            lexicon.morning.any { it.equals(phrase, ignoreCase = true) } -> MORNING_HOUR
            lexicon.afternoon.any { it.equals(phrase, ignoreCase = true) } -> AFTERNOON_HOUR
            else -> EVENING_HOUR
        }
        return today.atTime(hour, 0)
    }

    private fun Patterns.matchLiteralTime(input: String, claimed: MutableList<TaskToken>): LocalTime? {
        noon.find(input)?.takeIf { claimed.isFree(it.range) }?.let {
            claimed.claim(it.range, TaskTokenKind.DATE_TIME)
            return LocalTime.of(NOON_HOUR, 0)
        }
        midnight.find(input)?.takeIf { claimed.isFree(it.range) }?.let {
            claimed.claim(it.range, TaskTokenKind.DATE_TIME)
            return LocalTime.MIDNIGHT
        }
        return null
    }

    private fun Patterns.matchDate(
        input: String,
        claimed: MutableList<TaskToken>,
        today: LocalDate,
    ): MatchedDate? =
        matchRelativeDay(input, claimed, today)
            ?: matchNextWeek(input, claimed, today)
            ?: matchWeekday(input, claimed, today)
            ?: matchNumericDate(input, claimed, today)

    private fun Patterns.matchRelativeDay(
        input: String,
        claimed: MutableList<TaskToken>,
        today: LocalDate,
    ): MatchedDate? {
        val match = relativeDay.findAll(input).firstOrNull { claimed.isFree(it.range) } ?: return null
        claimed.claim(match.range, TaskTokenKind.DATE_TIME)
        val phrase = match.groupValues[1].collapsed()
        return when {
            lexicon.tomorrow.any { it.equals(phrase, ignoreCase = true) } -> MatchedDate(today.plusDays(1))
            lexicon.tonight.any { it.equals(phrase, ignoreCase = true) } ->
                MatchedDate(today, defaultHour = TONIGHT_HOUR)

            else -> MatchedDate(today)
        }
    }

    private fun Patterns.matchNextWeek(
        input: String,
        claimed: MutableList<TaskToken>,
        today: LocalDate,
    ): MatchedDate? {
        val match = nextWeek.find(input)?.takeIf { claimed.isFree(it.range) } ?: return null
        claimed.claim(match.range, TaskTokenKind.DATE_TIME)
        return MatchedDate(today.plusWeeks(1))
    }

    /** `next fri`, `by fri` and a bare `fri` all mean the next Friday that isn't today. */
    private fun Patterns.matchWeekday(
        input: String,
        claimed: MutableList<TaskToken>,
        today: LocalDate,
    ): MatchedDate? {
        val match = weekday.findAll(input).firstOrNull { claimed.isFree(it.range) } ?: return null
        val target = lexicon.weekdayOf(match.groupValues[1]) ?: return null
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

    private fun TaskLexicon.weekdayOf(word: String): DayOfWeek? =
        weekdays.entries.firstOrNull { it.key.equals(word, ignoreCase = true) }?.value

    // ── Assembly ─────────────────────────────────────────────────────────────

    private fun titleFrom(input: String, claimed: List<TaskToken>): String {
        val kept = StringBuilder(input.length)
        input.forEachIndexed { index, char ->
            if (claimed.none { index in it.range }) kept.append(char)
        }
        return kept.toString().collapsed()
    }

    private fun String.collapsed(): String = replace(WHITESPACE, " ").trim()

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

    /** One language's patterns, compiled once. See [WORD] for why none of them says `\b`. */
    private class Patterns(val lexicon: TaskLexicon) {

        private val clock = """(\d{1,2})(?::(\d{2}))?\s*(${lexicon.am.alt(lexicon.pm)})?"""

        /** `from 3pm to 5pm`, `3pm-5pm`, `3-5pm`, `15:00 until 17:00`. */
        val timeRange = compile(
            """(?:${lexicon.rangeFrom.alt()}\s+)?$clock\s*(?:-|–|—|${lexicon.rangeTo.alt()})\s*$clock""",
        )
        val relativeIn = compile(
            """${lexicon.relativeIn.alt()}\s+(\d+)\s*""" +
                """(${lexicon.minuteUnits.alt(lexicon.hourUnits, lexicon.dayUnits, lexicon.weekUnits)})""",
        )
        val duration = compile(
            """(?:${lexicon.durationFor.alt()}\s+)?""" +
                """(?:(\d+)\s*${lexicon.hourUnits.alt()}(?:\s*(\d{1,2})\s*${lexicon.minuteUnits.alt()}?)?""" +
                """|(\d+)\s*${lexicon.minuteUnits.alt()})""",
        )
        val timeMeridiem = compile(
            """(?:${lexicon.timeAt.alt()}\s+)?(\d{1,2})(?::(\d{2}))?\s*(${lexicon.am.alt(lexicon.pm)})""",
        )

        /** The `at` is the whole guard: without it `Read chapter 3` would become a time. */
        val timeAt = compile("""${lexicon.timeAt.alt()}\s+(\d{1,2})(?::(\d{2}))?""")
        val time24H = compile("""(?:${lexicon.timeAt.alt()}\s+)?(\d{1,2}):(\d{2})""")
        val noon = compile(lexicon.noon.alt())
        val midnight = compile(lexicon.midnight.alt())
        val dayPart = compile("""(${lexicon.morning.alt(lexicon.afternoon, lexicon.evening)})""")
        val relativeDay = compile("""(${lexicon.today.alt(lexicon.tonight, lexicon.tomorrow)})""")
        val nextWeek = compile(lexicon.nextWeek.alt())
        val weekday = compile(
            """${optional(lexicon.weekdayPrefixes, before = true)}(${lexicon.weekdays.keys.toList().alt()})""" +
                optional(lexicon.weekdaySuffixes, before = false),
        )

        /** Every pattern here begins and ends on a word, so the boundary is the same on both sides. */
        private fun compile(pattern: String) =
            Regex("$NOT_AFTER_WORD$pattern$NOT_BEFORE_WORD", RegexOption.IGNORE_CASE)

        /**
         * A self-contained alternation of literals, longest first — regex alternation is
         * leftmost-first, so a bare `م` listed before `مساء` would swallow it. The group is part of
         * the result because callers concatenate `\s+` onto it, and `(?:a|b)\s+` is not `a|b\s+`.
         * Spaces inside a phrase become `\s+` so it survives however the user spaced it.
         */
        private fun List<String>.alt(vararg others: List<String>): String =
            (this + others.flatMap { it })
                .sortedByDescending { it.length }
                .joinToString(separator = "|", prefix = "(?:", postfix = ")") { phrase ->
                    phrase.trim().split(' ').joinToString("""\s+""") { Pattern.quote(it) }
                }

        /** Dropped entirely when the language has no such qualifier, so it cannot eat a space. */
        private fun optional(words: List<String>, before: Boolean): String = when {
            words.isEmpty() -> ""
            before -> """(?:${words.alt()}\s+)?"""
            else -> """(?:\s+${words.alt()})?"""
        }
    }
}
