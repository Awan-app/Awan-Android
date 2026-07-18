package com.awan.feature.onboarding.impl.ui

import com.awan.app.core.model.DayBounds
import java.util.Calendar
import java.util.Locale

/** 12-hour clock, e.g. "7:00 AM" / "11:30 PM". */
fun formatClock(minutes: Int): String {
    val m = minutes.mod(DayBounds.MINUTES_PER_DAY)
    val hour24 = m / 60
    val minute = m % 60
    val period = if (hour24 < 12) "AM" else "PM"
    val hour12 = when (val h = hour24 % 12) {
        0 -> 12
        else -> h
    }
    return String.format(Locale.US, "%d:%02d %s", hour12, minute, period)
}

/** Human label for the task-length control, e.g. "About 1½ hours". */
fun humanDuration(minutes: Int): String = when {
    minutes < 60 -> "$minutes minutes"
    minutes % 60 == 0 && minutes == 60 -> "About 1 hour"
    minutes % 60 == 0 -> "About ${minutes / 60} hours"
    minutes % 60 == 30 && minutes / 60 == 1 -> "About 1½ hours"
    else -> "About ${minutes / 60}½ hours"
}

fun timeOfDayGreeting(hourOfDay: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): String = when (hourOfDay) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    else -> "Good evening"
}
