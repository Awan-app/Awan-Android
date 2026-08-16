package com.awan.app.core.notifications.model

import java.time.LocalDateTime

/**
 * A session's schedule as it stood at one moment.
 *
 * Scopes a dismissal: pressing "Stop" on the running-session notification silences it for the session
 * *as it was*, so moving or snoozing that session brings the notification back rather than leaving the
 * user silently without a live update for a block they just re-planned.
 */
data class SessionWindow(
    val start: LocalDateTime,
    val end: LocalDateTime,
)
