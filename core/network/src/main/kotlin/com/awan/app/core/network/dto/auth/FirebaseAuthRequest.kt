package com.awan.app.core.network.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FirebaseAuthRequest(
    @SerialName("idToken") val idToken: String,
    @SerialName("deviceId") val deviceId: String,
)
