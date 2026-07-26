package com.awan.app.core.data.util

import com.awan.app.core.model.DayBounds
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.util.Locale

/** Minutes-from-midnight to the `HH:mm:ss` the API expects. Wraps, so 1500 becomes `01:00:00`. */
internal fun formatMinutesToTime(minutes: Int): String {
    val totalMinutes = minutes.mod(DayBounds.MINUTES_PER_DAY)
    return String.format(Locale.US, "%02d:%02d:00", totalMinutes / 60, totalMinutes % 60)
}

/** Inverse of [formatMinutesToTime]. Null when the API sends something that is not `HH:mm[:ss]`. */
internal fun parseTimeToMinutes(time: String): Int? {
    val parts = time.split(":")
    if (parts.size < 2) return null
    val hours = parts[0].toIntOrNull() ?: return null
    val minutes = parts[1].toIntOrNull() ?: return null
    return hours * 60 + minutes
}

/**
 * Session timestamps come back as ISO datetimes. An offset-bearing value is converted to the
 * device's zone first — reading the wall-clock digits straight off the string would show the
 * user server time labelled as their own.
 */
internal fun parseIsoDateTime(value: String): LocalDateTime? =
    runCatching { OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime() }
        .recoverCatching { LocalDateTime.parse(value) }
        .getOrNull()

internal fun LocalDateTime.minutesOfDay(): Int = hour * 60 + minute
