package com.awan.app.core.network.dto.auth

import com.awan.app.core.network.dto.user.UserDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VerifyOtpResponse(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("refreshToken") val refreshToken: String,
    @SerialName("accessTokenExpiresIn") val accessTokenExpiresIn: Long? = null,
    @SerialName("user") val user: UserDto? = null,
)
