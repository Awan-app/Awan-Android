package com.awan.app.core.model

import java.time.LocalDateTime

data class SessionDraft(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val zoneId: String? = null,
)
