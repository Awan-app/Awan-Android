package com.awan.feature.onboarding.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.awan.app.core.model.DayBounds
import com.awan.feature.onboarding.impl.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** 12-hour clock, e.g. "7:00 AM"; the AM/PM marker follows [locale]. */
fun formatClock(minutes: Int, locale: Locale = Locale.getDefault()): String {
    val m = minutes.mod(DayBounds.MINUTES_PER_DAY)
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, m / 60)
        set(Calendar.MINUTE, m % 60)
    }
    return SimpleDateFormat("h:mm a", locale).format(calendar.time)
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
