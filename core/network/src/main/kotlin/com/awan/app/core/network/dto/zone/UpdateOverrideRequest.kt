package com.awan.app.core.network.dto.zone

import kotlinx.serialization.Serializable

@Serializable
data class UpdateOverrideRequest(
    val name: String? = null,
    val dateOfDay: String
)
