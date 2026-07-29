package com.awan.app.core.model

import java.time.LocalDateTime

/** A session that doesn't exist server-side yet, so it has no id. [end] must be after [start]. */
data class SessionDraft(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val zoneId: String? = null,
)
