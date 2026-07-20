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

@Serializable
data class UserDto(
    @SerialName("id") val id: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("isNew") val isNew: Boolean? = null,
)


@Serializable
data class RequestOtpRequest(
    @SerialName("email") val email: String,
)


@Serializable
data class VerifyOtpRequest(
    @SerialName("email") val email: String,
    @SerialName("code") val code: String,
    @SerialName("deviceId") val deviceId: String,
)

@Serializable
data class VerifyOtpResponse(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("refreshToken") val refreshToken: String,
    @SerialName("accessTokenExpiresIn") val accessTokenExpiresIn: Long? = null,
    @SerialName("user") val user: UserDto? = null,
)


@Serializable
data class RefreshTokenRequest(
    @SerialName("refreshToken") val refreshToken: String,
    @SerialName("deviceId") val deviceId: String,
)


@Serializable
data class LogoutRequest(
    @SerialName("deviceId") val deviceId: String,
)

@Serializable
data class ApiErrorInfo(
    @SerialName("remainingAttempts") val remainingAttempts: Int? = null,
)

@Serializable
data class ApiErrorResponse(
    @SerialName("message") val message: String? = null,
    @SerialName("statusCode") val statusCode: Int? = null,
    @SerialName("errorCode") val errorCode: String? = null,
    @SerialName("info") val info: ApiErrorInfo? = null,
    @SerialName("timestamp") val timestamp: String? = null,
    @SerialName("code") val code: String? = null,
    @SerialName("details") val details: List<String>? = null,
)
