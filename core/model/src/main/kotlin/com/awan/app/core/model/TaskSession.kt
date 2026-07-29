package com.awan.app.core.model

import java.time.LocalDateTime

enum class SessionStatus {
    SCHEDULED,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED,
    CANCELLED,

    /** The backend sent a status this build doesn't know about. */
    UNKNOWN,
}

/**
 * A concrete occurrence of a [Task] on the calendar. [start]/[end] are wall-clock local times with
 * no offset — that is exactly what the backend stores, so converting to an instant here would
 * invent information the server never sent.
 */
data class TaskSession(
    val id: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val status: SessionStatus = SessionStatus.SCHEDULED,
    val locked: Boolean = false,
    val zoneId: String? = null,
)
