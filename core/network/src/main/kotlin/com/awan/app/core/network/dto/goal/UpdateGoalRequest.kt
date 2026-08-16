package com.awan.app.core.network.dto.goal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateGoalRequest(
    @SerialName("title") val title: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("targetDate") val targetDate: String? = null,
)
