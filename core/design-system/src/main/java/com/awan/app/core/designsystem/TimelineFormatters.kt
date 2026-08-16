package com.awan.app.core.designsystem

import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
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

fun LocalTime.toMinutesOfDay(): Int = hour * 60 + minute

/**
 * Robustly parses date/time from ISO strings, handling UTC strings ('Z' suffix),
 * offset strings ('+03:00'), and local strings ('2026-08-13T10:00:00').
 * For strings with timezone/offset, converts to the given [zoneId] (defaulting to system default).
 */
fun parseIsoDateTime(value: String, zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime? {
    if (value.isBlank()) return null
    return runCatching {
        OffsetDateTime.parse(value).atZoneSameInstant(zoneId).toLocalDateTime()
    }.recoverCatching {
        ZonedDateTime.parse(value).withZoneSameInstant(zoneId).toLocalDateTime()
    }.recoverCatching {
        Instant.parse(value).atZone(zoneId).toLocalDateTime()
    }.recoverCatching {
        LocalDateTime.parse(value)
    }.getOrNull()
}
