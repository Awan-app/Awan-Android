@file:Suppress("NewApi")

package com.awan.feature.home.impl.ui.components

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun calculateDurationMinutes(start: LocalDateTime, end: LocalDateTime): Int {
    return java.time.Duration.between(start, end).toMinutes().toInt()
}

internal fun calculateDurationMinutes(startIso: String, endIso: String): Int? {
    return try {
        val start = LocalDateTime.parse(startIso)
        val end = LocalDateTime.parse(endIso)
        calculateDurationMinutes(start, end)
    } catch (e: Exception) {
        null
    }
}

internal fun calculateEnd(start: LocalDateTime, durationMinutes: Int): LocalDateTime {
    return start.plusMinutes(durationMinutes.toLong())
}

internal fun calculateEndIso(startIso: String, durationMinutes: Int): String? {
    return try {
        val start = LocalDateTime.parse(startIso)
        calculateEnd(start, durationMinutes).toString()
    } catch (e: Exception) {
        null
    }
}

internal fun formatIsoDate(isoString: String): String {
    return try {
        val dt = LocalDateTime.parse(isoString)
        formatDate(dt)
    } catch (e: Exception) {
        try {
            val date = LocalDate.parse(isoString.take(10))
            date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
        } catch (e: Exception) {
            isoString
        }
    }
}

internal fun formatDate(date: LocalDateTime): String {
    return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
}

internal fun formatIsoTime(isoString: String): String {
    return try {
        val dt = LocalDateTime.parse(isoString)
        formatTime(dt)
    } catch (e: Exception) {
        isoString
    }
}

internal fun formatTime(time: LocalDateTime): String {
    return time.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
}
