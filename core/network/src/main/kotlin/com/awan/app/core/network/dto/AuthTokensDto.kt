package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthTokensDto(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("refreshToken") val refreshToken: String,
    @SerialName("accessTokenExpiresIn") val expiresIn: Long? = null,
    @SerialName("tokenType") val tokenType: String = "Bearer",
)
