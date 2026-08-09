package com.awan.app.core.network.dto.session

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Times only — status changes go through the dedicated complete/uncomplete/cancel endpoints. */
@Serializable
data class UpdateSessionRequest(
    @SerialName("start") val start: String? = null,
    @SerialName("end") val end: String? = null,
    @SerialName("locked") val locked: Boolean? = null,
)
