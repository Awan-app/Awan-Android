package com.awan.app.core.model

import java.time.LocalDateTime

data class TaskSession(
    val id: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val status: SessionStatus = SessionStatus.UNKNOWN,
    val locked: Boolean = false,
    val zoneId: String? = null,
)
