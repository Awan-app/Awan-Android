package com.awan.app.core.data.notifications.mapper

import com.awan.app.core.database.model.UpcomingSessionRow
import com.awan.app.core.domain.notifications.model.UpcomingSession
import com.awan.app.core.model.SessionStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeParseException

/**
 * Rows whose date/time cannot be parsed are dropped rather than defaulted. A session that silently
 * became "midnight today" would fire a notification at the wrong time, which is worse than firing
 * none — and the row is unusable for scheduling either way.
 */
fun List<UpcomingSessionRow>.toUpcomingSessions(): List<UpcomingSession> = mapNotNull { it.toUpcomingSessionOrNull() }

private fun UpcomingSessionRow.toUpcomingSessionOrNull(): UpcomingSession? {
    val date = parseDate(date) ?: return null
    val start = parseTime(startTime) ?: return null
    val end = parseTime(endTime) ?: return null

    val startDateTime = LocalDateTime.of(date, start)
    // A session dragged past midnight keeps the start date, so an end before the start belongs to
    // the following day rather than to a negative-length session.
    val endDateTime = LocalDateTime.of(date, end)
        .let { if (it.isBefore(startDateTime)) it.plusDays(1) else it }

    return UpcomingSession(
        id = id,
        taskId = taskId,
        title = title,
        start = startDateTime,
        end = endDateTime,
        status = parseStatus(status),
        zoneId = zoneId,
    )
}

private fun parseDate(raw: String): LocalDate? = try {
    LocalDate.parse(raw)
} catch (_: DateTimeParseException) {
    null
}

private fun parseTime(raw: String): LocalTime? = try {
    LocalTime.parse(raw)
} catch (_: DateTimeParseException) {
    null
}

private fun parseStatus(raw: String): SessionStatus = when (raw.uppercase()) {
    "SCHEDULED" -> SessionStatus.SCHEDULED
    "IN_PROGRESS" -> SessionStatus.IN_PROGRESS
    "COMPLETED" -> SessionStatus.COMPLETED
    "CANCELLED" -> SessionStatus.CANCELLED
    "MISSED" -> SessionStatus.MISSED
    else -> SessionStatus.UNKNOWN
}
