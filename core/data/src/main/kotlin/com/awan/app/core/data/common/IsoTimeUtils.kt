package com.awan.app.core.data.common

import java.time.LocalDate
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
    parseIso(isoDateTime)?.let { return it.toLocalTime().asStoredTime() }
    // Already just a time portion.
    return try {
        LocalTime.parse(isoDateTime).asStoredTime()
    } catch (_: DateTimeParseException) {
        fallback
    }
}

/** Both shapes the API sends: with an offset, and — the common one — without. */
private fun parseIso(isoDateTime: String): LocalDateTime? = try {
    OffsetDateTime.parse(isoDateTime).toLocalDateTime()
} catch (_: DateTimeParseException) {
    try {
        LocalDateTime.parse(isoDateTime)
    } catch (_: DateTimeParseException) {
        null
    }
}

/** Rows are `HH:mm:ss`; `createdAt`-style sub-second precision must not leak into one. */
private fun LocalTime.asStoredTime(): String =
    truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_LOCAL_TIME)

/**
 * Extracts the date portion (YYYY-MM-DD) from an ISO datetime string.
 * Falls back to [fallback] if the string cannot be parsed.
 *
 * Parsed rather than sliced at ten characters: the offset-free form the API sends only survived the
 * slice by luck, and anything else of that length — `"10/08/2026 14:30"` — became a `date` column
 * Room accepts and `LocalDate.parse` later throws on, taking the schedule Flow down with it.
 */
internal fun extractDateFromIso(isoDateTime: String, fallback: String = ""): String {
    parseIso(isoDateTime)?.let { return it.toLocalDate().toString() }
    return try {
        LocalDate.parse(isoDateTime).toString()
    } catch (_: DateTimeParseException) {
        fallback
    }
}
