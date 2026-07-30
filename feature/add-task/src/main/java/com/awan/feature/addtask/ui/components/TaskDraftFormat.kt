package com.awan.feature.addtask.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.awan.feature.addtask.R
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val MINUTES_PER_HOUR = 60

/**
 * "Today" / "Today at 6:00 PM" / "Mon, 27 Jul at 4:00 PM", in the device's locale.
 *
 * [showTime] is false when the sentence named a day but no clock time, so the chip doesn't present
 * the parser's 9am fallback as though the user had chosen it.
 */
@Composable
fun rememberWhenLabel(startAt: LocalDateTime, today: LocalDate, showTime: Boolean = true): String {
    val locale = LocalConfiguration.current.locales[0]
    val date = startAt.toLocalDate()
    val dayLabel = when (date) {
        today -> stringResource(R.string.add_task_day_today)
        today.plusDays(1) -> stringResource(R.string.add_task_day_tomorrow)
        else -> date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    }
    if (!showTime) return dayLabel

    val time = startAt.toLocalTime()
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
    return stringResource(R.string.add_task_day_at_time, dayLabel, time)
}

@Composable
fun durationLabel(minutes: Int): String {
    val hours = minutes / MINUTES_PER_HOUR
    val remainder = minutes % MINUTES_PER_HOUR
    return when {
        hours == 0 -> stringResource(R.string.add_task_duration_minutes, remainder)
        remainder == 0 -> stringResource(R.string.add_task_duration_hours, hours)
        else -> stringResource(R.string.add_task_duration_hours_minutes, hours, remainder)
    }
}
