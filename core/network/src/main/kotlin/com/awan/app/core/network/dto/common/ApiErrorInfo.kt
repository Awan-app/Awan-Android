package com.awan.app.core.network.dto.common

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorInfo(
    @SerialName("remainingAttempts") val remainingAttempts: Int? = null,
    @SerialName("retryAfterSeconds") val retryAfterSeconds: Int? = null,
)
