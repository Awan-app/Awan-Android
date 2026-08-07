@file:Suppress("NewApi")

package com.awan.feature.home.impl.ui.components

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun calculateDurationMinutes(startIso: String, endIso: String): Int? {
    return try {
        val start = LocalDateTime.parse(startIso)
        val end = LocalDateTime.parse(endIso)
        java.time.Duration.between(start, end).toMinutes().toInt()
    } catch (e: Exception) {
        null
    }
}

internal fun formatIsoDate(isoString: String): String {
    return try {
        val dt = LocalDateTime.parse(isoString)
        dt.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))
    } catch (e: Exception) {
        try {
            val date = LocalDate.parse(isoString.take(10))
            date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))
        } catch (e: Exception) {
            isoString
        }
    }
}

internal fun formatIsoTime(isoString: String): String {
    return try {
        val dt = LocalDateTime.parse(isoString)
        dt.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH))
    } catch (e: Exception) {
        isoString
    }
}
