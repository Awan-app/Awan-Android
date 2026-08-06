package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PageResponse<T>(
    @SerialName("content") val content: List<T> = emptyList(),
    @SerialName("totalElements") val totalElements: Int = 0,
    @SerialName("totalPages") val totalPages: Int = 0,
    @SerialName("size") val size: Int = 0,
    @SerialName("number") val number: Int = 0,
)
