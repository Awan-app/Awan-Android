package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
