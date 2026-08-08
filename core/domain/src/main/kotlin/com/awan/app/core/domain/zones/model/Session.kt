package com.awan.app.core.domain.zones.model

import com.awan.app.core.model.Category
import java.time.LocalDateTime

data class Session(
    val id: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val status: String,
    val locked: Boolean,
    val zoneId: String? = null,
    val taskId: String? = null,
    val category: Category? = null
)
