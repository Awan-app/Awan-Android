package com.awan.app.core.network.dto.devicetoken

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceTokenRequest(
    @SerialName("deviceId") val deviceId: String,
    @SerialName("fcmToken") val fcmToken: String,
    @SerialName("deviceType") val deviceType: String = "ANDROID",
)

@Serializable
data class DeviceTokenResponse(
    @SerialName("id") val id: String,
    @SerialName("deviceId") val deviceId: String,
    @SerialName("deviceType") val deviceType: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String,
)
