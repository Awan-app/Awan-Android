package com.awan.feature.onboarding.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.awan.app.core.model.DayBounds
import com.awan.feature.onboarding.impl.R
import java.util.Calendar
import java.util.Locale

/** A wall-clock reading split into its 12-hour parts. [hour12] is 1..12, never 0. */
internal data class ClockParts(val hour12: Int, val minute: Int, val isAm: Boolean)

/** Pure 24h→12h conversion, wrapping any out-of-range [minutes] into the day. */
internal fun clockParts(minutes: Int): ClockParts {
    val m = minutes.mod(DayBounds.MINUTES_PER_DAY)
    val hour24 = m / 60
    return ClockParts(
        hour12 = if (hour24 % 12 == 0) 12 else hour24 % 12,
        minute = m % 60,
        isAm = hour24 < 12,
    )
}

/**
 * 12-hour clock, e.g. "7:00 AM". Digits are formatted with [Locale.ROOT] so they stay Latin in
 * every locale: Baloo2 has no Arabic-Indic glyphs, and locale-supplied digits fall back to a system
 * font mid-label, which visibly changes the size and shape of the time as it ticks. Only the
 * AM/PM marker is localized.
 */
@Composable
fun formatClock(minutes: Int): String {
    val (hour12, minute, isAm) = clockParts(minutes)
    val marker = stringResource(if (isAm) R.string.onboarding_clock_am else R.string.onboarding_clock_pm)
    return String.format(Locale.ROOT, stringResource(R.string.onboarding_clock_format), hour12, minute, marker)
}

/** Human label for the task-length control, e.g. "About 1½ hours". */
@Composable
fun humanDuration(minutes: Int): String = when {
    minutes < 60 -> pluralStringResource(R.plurals.onboarding_duration_minutes, minutes, minutes)
    minutes % 60 == 0 -> pluralStringResource(R.plurals.onboarding_duration_about_hours, minutes / 60, minutes / 60)
    else -> stringResource(R.string.onboarding_duration_about_half_hours, minutes / 60)
}

@Composable
fun timeOfDayGreeting(hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): String = when (hourOfDay) {
    in 5..11 -> stringResource(R.string.onboarding_greeting_morning)
    in 12..17 -> stringResource(R.string.onboarding_greeting_afternoon)
    else -> stringResource(R.string.onboarding_greeting_evening)
}
