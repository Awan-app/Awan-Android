package com.awan.app.core.network.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class SessionDto(
    val id: String,
    val start: String,
    val end: String,
    val status: String,
    val locked: Boolean,
    val zoneId: String? = null,
    val taskId: String? = null
)
