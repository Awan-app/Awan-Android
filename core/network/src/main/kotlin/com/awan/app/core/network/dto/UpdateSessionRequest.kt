package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateSessionRequest(
    @SerialName("start") val start: String? = null,
    @SerialName("end") val end: String? = null,
    @SerialName("status") val status: String? = null,
)
