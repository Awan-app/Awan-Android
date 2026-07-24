package com.awan.app.core.designsystem

import java.util.Locale

fun formatHourTo12h(hour: Int): String {
    val modHour = hour % 24
    val amPm = if (modHour >= 12 && modHour < 24) "PM" else "AM"
    val hour12 = when {
        modHour == 0 -> 12
        modHour > 12 -> modHour - 12
        else -> modHour
    }
    return "$hour12 $amPm"
}

fun formatMinutesToRange(startMinutes: Int, durationMinutes: Int): String {
    val endMinutes = startMinutes + durationMinutes
    return "${formatTime(startMinutes)} - ${formatTime(endMinutes)}"
}

fun formatTime(minutes: Int): String {
    val totalMins = minutes.mod(24 * 60)
    val hour24 = totalMins / 60
    val mins = totalMins % 60
    val amPm = if (hour24 >= 12) "PM" else "AM"
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    return String.format(Locale.US, "%d:%02d %s", hour12, mins, amPm)
}
