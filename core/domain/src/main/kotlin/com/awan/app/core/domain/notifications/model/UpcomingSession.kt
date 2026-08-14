package com.awan.app.core.domain.notifications.model

import com.awan.app.core.model.SessionStatus
import java.time.LocalDateTime

/**
 * A session the notification scheduler may need to fire for, with its start and end already
 * assembled into [LocalDateTime]s — the cached row stores date and time as separate strings.
 *
 * Times stay local rather than absolute on purpose: a session is at 09:00 wherever the user is, so
 * the instant is resolved against the current zone at schedule time and re-resolved when the device
 * timezone changes.
 */
data class UpcomingSession(
    val id: String,
    val taskId: String,
    val title: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val status: SessionStatus,
    val zoneId: String?,
)
