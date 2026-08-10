package com.awan.app.core.data.common

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/**
 * Extracts the time portion (HH:mm:ss) from an ISO datetime string.
 * Falls back to [fallback] if the string cannot be parsed.
 *
 * The offset-free form is the one the API actually sends (`"2026-07-22T09:00:00"`), and it parses as
 * neither [OffsetDateTime] nor [LocalTime] — leaving it out made every caller silently keep its
 * fallback, so a moved session wrote its old time straight back to Room.
 */
internal fun extractTimeFromIso(isoDateTime: String, fallback: String = "00:00:00"): String {
    return try {
        OffsetDateTime.parse(isoDateTime).toLocalTime().asStoredTime()
    } catch (_: DateTimeParseException) {
        try {
            LocalDateTime.parse(isoDateTime).toLocalTime().asStoredTime()
        } catch (_: DateTimeParseException) {
            // Already just a time portion.
            try {
                LocalTime.parse(isoDateTime).asStoredTime()
            } catch (_: DateTimeParseException) {
                fallback
            }
        }
    }
}

/** Rows are `HH:mm:ss`; `createdAt`-style sub-second precision must not leak into one. */
private fun LocalTime.asStoredTime(): String =
    truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_LOCAL_TIME)

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
