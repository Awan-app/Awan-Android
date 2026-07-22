package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VerifyOtpRequest(
    @SerialName("email") val email: String,
    @SerialName("code") val code: String,
    @SerialName("deviceId") val deviceId: String,
)
