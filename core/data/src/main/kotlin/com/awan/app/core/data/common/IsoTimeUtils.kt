package com.awan.app.core.data.common

import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Extracts the time portion (HH:mm:ss) from an ISO datetime string.
 * Falls back to [fallback] if the string cannot be parsed.
 */
internal fun extractTimeFromIso(isoDateTime: String, fallback: String = "00:00:00"): String {
    return try {
        val odt = OffsetDateTime.parse(isoDateTime)
        odt.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME)
    } catch (_: DateTimeParseException) {
        // Try just the time portion for already-extracted times
        try {
            LocalTime.parse(isoDateTime)
            isoDateTime
        } catch (_: DateTimeParseException) {
            fallback
        }
    }
}

/**
 * Extracts the date portion (YYYY-MM-DD) from an ISO datetime string.
 * Falls back to [fallback] if the string cannot be parsed.
 */
internal fun extractDateFromIso(isoDateTime: String, fallback: String = ""): String {
    return try {
        val odt = OffsetDateTime.parse(isoDateTime)
        odt.toLocalDate().toString()
    } catch (_: DateTimeParseException) {
        if (isoDateTime.length >= 10) isoDateTime.substring(0, 10) else fallback
    }
}
