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

/** "Today 6:00 PM" / "Tomorrow 6:00 PM" / "Mon, 27 Jul 4:00 PM", in the device's locale. */
@Composable
fun rememberWhenLabel(startAt: LocalDateTime, today: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    val time = startAt.toLocalTime()
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
    val date = startAt.toLocalDate()

    return when (date) {
        today -> stringResource(R.string.add_task_when_today, time)
        today.plusDays(1) -> stringResource(R.string.add_task_when_tomorrow, time)
        else -> stringResource(
            R.string.add_task_when_dated,
            date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)),
            time,
        )
    }
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
