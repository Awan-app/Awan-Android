package com.awan.app.core.domain.zones.model

data class Session(
    val id: String,
    val start: String,
    val end: String,
    val status: String,
    val locked: Boolean,
    val zoneId: String? = null,
    val taskId: String? = null
)
