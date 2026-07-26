package com.awan.app.core.designsystem

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun formatHourTo12h(hour: Int): String {
    val modHour = hour.mod(24)
    val time = LocalTime.of(modHour, 0)
    val formatter = DateTimeFormatter.ofPattern("h a", Locale.getDefault())
    return time.format(formatter)
}

fun formatMinutesToRange(startMinutes: Int, durationMinutes: Int): String {
    val endMinutes = startMinutes + durationMinutes
    return "${formatTime(startMinutes)} - ${formatTime(endMinutes)}"
}

fun formatTime(minutes: Int): String {
    val totalMins = minutes.mod(24 * 60)
    val hour = totalMins / 60
    val min = totalMins % 60
    val time = LocalTime.of(hour, min)
    val formatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
    return time.format(formatter)
}
