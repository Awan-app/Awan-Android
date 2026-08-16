package com.awan.app.core.domain.task.parser

import java.time.DayOfWeek
import java.util.Locale

/**
 * Every word [TaskInputParser] reads and [TaskInputWriter] writes, in one language.
 *
 * The two halves share this table on purpose: the writer's contract is that whatever it emits, the
 * parser reads back to the same value. Anything the writer can produce (`write…`) is therefore also
 * listed among the forms the parser accepts.
 *
 * `:core:domain` is Android-free, so these cannot come from `strings.xml` — they are grammar, not
 * display copy, and they have to sit next to the regexes that consume them.
 */
@Suppress("LongParameterList")
class TaskLexicon(
    val weekdays: Map<String, DayOfWeek>,
    val hourUnits: List<String>,
    val minuteUnits: List<String>,
    val dayUnits: List<String>,
    val weekUnits: List<String>,
    val am: List<String>,
    val pm: List<String>,
    /** `from` 3pm to 5pm. */
    val rangeFrom: List<String>,
    /** From 3pm `to` 5pm. */
    val rangeTo: List<String>,
    /** `in` 20 minutes. */
    val relativeIn: List<String>,
    /** `for` 45 min. */
    val durationFor: List<String>,
    /** `at` 3pm. */
    val timeAt: List<String>,
    val noon: List<String>,
    val midnight: List<String>,
    /** Whole phrases, not bare nouns: `this morning` names a day, a bare `morning` does not. */
    val morning: List<String>,
    val afternoon: List<String>,
    val evening: List<String>,
    val today: List<String>,
    val tonight: List<String>,
    val tomorrow: List<String>,
    val nextWeek: List<String>,
    /** English qualifies a weekday before it (`next fri`)… */
    val weekdayPrefixes: List<String>,
    /** …Arabic after it (`الجمعة القادمة`). Either way the qualifier is consumed, not interpreted. */
    val weekdaySuffixes: List<String>,
    val writeToday: String,
    val writeTomorrow: String,
    val writeAt: String,
    val writeFor: String,
    val writeAm: String,
    val writePm: String,
    val writeHour: String,
    val writeHours: String,
    val writeMinutes: String,
    /** Joins `1h30`. Null writes the total in minutes instead, for languages with no such shorthand. */
    val hourMinuteSeparator: String?,
    val writeWeekday: Map<DayOfWeek, String>,
) {
    fun meridiemOf(word: String): Meridiem? = when {
        word.isEmpty() -> null
        am.containsIgnoringCase(word) -> Meridiem.AM
        pm.containsIgnoringCase(word) -> Meridiem.PM
        else -> null
    }

    fun unitOf(word: String): TimeUnit? = when {
        weekUnits.containsIgnoringCase(word) -> TimeUnit.WEEK
        dayUnits.containsIgnoringCase(word) -> TimeUnit.DAY
        hourUnits.containsIgnoringCase(word) -> TimeUnit.HOUR
        minuteUnits.containsIgnoringCase(word) -> TimeUnit.MINUTE
        else -> null
    }

    private fun List<String>.containsIgnoringCase(word: String) = any { it.equals(word, ignoreCase = true) }

    companion object {
        val English = TaskLexicon(
            weekdays = mapOf(
                "mon" to DayOfWeek.MONDAY, "monday" to DayOfWeek.MONDAY,
                "tue" to DayOfWeek.TUESDAY, "tues" to DayOfWeek.TUESDAY, "tuesday" to DayOfWeek.TUESDAY,
                "wed" to DayOfWeek.WEDNESDAY, "weds" to DayOfWeek.WEDNESDAY, "wednesday" to DayOfWeek.WEDNESDAY,
                "thu" to DayOfWeek.THURSDAY, "thur" to DayOfWeek.THURSDAY, "thurs" to DayOfWeek.THURSDAY,
                "thursday" to DayOfWeek.THURSDAY,
                "fri" to DayOfWeek.FRIDAY, "friday" to DayOfWeek.FRIDAY,
                "sat" to DayOfWeek.SATURDAY, "saturday" to DayOfWeek.SATURDAY,
                "sun" to DayOfWeek.SUNDAY, "sunday" to DayOfWeek.SUNDAY,
            ),
            hourUnits = listOf("h", "hr", "hrs", "hour", "hours"),
            minuteUnits = listOf("m", "min", "mins", "minute", "minutes"),
            dayUnits = listOf("d", "day", "days"),
            weekUnits = listOf("w", "week", "weeks"),
            am = listOf("am"),
            pm = listOf("pm"),
            rangeFrom = listOf("from"),
            rangeTo = listOf("to", "until", "till"),
            relativeIn = listOf("in"),
            durationFor = listOf("for"),
            timeAt = listOf("at"),
            noon = listOf("noon"),
            midnight = listOf("midnight"),
            morning = listOf("this morning"),
            afternoon = listOf("this afternoon"),
            evening = listOf("this evening"),
            today = listOf("today"),
            tonight = listOf("tonight"),
            tomorrow = listOf("tomorrow"),
            nextWeek = listOf("next week"),
            weekdayPrefixes = listOf("next", "by", "on"),
            weekdaySuffixes = emptyList(),
            writeToday = "today",
            writeTomorrow = "tomorrow",
            writeAt = "at",
            writeFor = "for",
            writeAm = "am",
            writePm = "pm",
            writeHour = "hour",
            writeHours = "hours",
            writeMinutes = "min",
            hourMinuteSeparator = "h",
            writeWeekday = DayOfWeek.entries.associateWith { it.name.lowercase() },
        )

        /**
         * Both hamza spellings of every weekday are accepted because keyboards and habits differ, and
         * only the `ال`-prefixed forms are: a bare `اثنين` is the number two far more often than it is
         * Monday.
         */
        val Arabic = TaskLexicon(
            weekdays = mapOf(
                "الاثنين" to DayOfWeek.MONDAY, "الإثنين" to DayOfWeek.MONDAY,
                "الثلاثاء" to DayOfWeek.TUESDAY,
                "الاربعاء" to DayOfWeek.WEDNESDAY, "الأربعاء" to DayOfWeek.WEDNESDAY,
                "الخميس" to DayOfWeek.THURSDAY,
                "الجمعة" to DayOfWeek.FRIDAY,
                "السبت" to DayOfWeek.SATURDAY,
                "الاحد" to DayOfWeek.SUNDAY, "الأحد" to DayOfWeek.SUNDAY,
            ),
            hourUnits = listOf("س", "ساعة", "ساعات"),
            minuteUnits = listOf("د", "دقيقة", "دقائق"),
            dayUnits = listOf("ي", "يوم", "أيام", "ايام"),
            weekUnits = listOf("أسبوع", "اسبوع", "أسابيع", "اسابيع"),
            am = listOf("ص", "صباحا", "صباحًا", "صباح"),
            pm = listOf("م", "مساء", "مساءً", "مساءا"),
            rangeFrom = listOf("من"),
            rangeTo = listOf("إلى", "الى", "حتى", "لغاية"),
            relativeIn = listOf("بعد"),
            durationFor = listOf("لمدة"),
            timeAt = listOf("الساعة", "في الساعة", "عند"),
            noon = listOf("الظهر", "ظهرا", "ظهرًا"),
            midnight = listOf("منتصف الليل"),
            morning = listOf("هذا الصباح", "صباح اليوم"),
            afternoon = listOf("بعد الظهر", "هذا بعد الظهر"),
            evening = listOf("هذا المساء", "مساء اليوم"),
            today = listOf("اليوم"),
            tonight = listOf("الليلة", "هذه الليلة"),
            tomorrow = listOf("غدا", "غدًا", "بكرة"),
            nextWeek = listOf("الأسبوع القادم", "الاسبوع القادم", "الأسبوع المقبل", "الاسبوع المقبل"),
            weekdayPrefixes = listOf("يوم"),
            weekdaySuffixes = listOf("القادم", "القادمة", "المقبل", "المقبلة"),
            writeToday = "اليوم",
            writeTomorrow = "غدا",
            writeAt = "الساعة",
            writeFor = "لمدة",
            writeAm = "ص",
            writePm = "م",
            writeHour = "ساعة",
            writeHours = "ساعات",
            writeMinutes = "دقيقة",
            // `1س30` is not how anyone writes an hour and a half, so Arabic writes 90 دقيقة instead.
            hourMinuteSeparator = null,
            writeWeekday = mapOf(
                DayOfWeek.MONDAY to "الاثنين",
                DayOfWeek.TUESDAY to "الثلاثاء",
                DayOfWeek.WEDNESDAY to "الأربعاء",
                DayOfWeek.THURSDAY to "الخميس",
                DayOfWeek.FRIDAY to "الجمعة",
                DayOfWeek.SATURDAY to "السبت",
                DayOfWeek.SUNDAY to "الأحد",
            ),
        )

        fun of(locale: Locale): TaskLexicon = if (locale.language == "ar") Arabic else English
    }
}

enum class Meridiem { AM, PM }

enum class TimeUnit { MINUTE, HOUR, DAY, WEEK }
