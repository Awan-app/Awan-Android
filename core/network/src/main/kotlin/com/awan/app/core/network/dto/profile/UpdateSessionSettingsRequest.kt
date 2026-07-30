package com.awan.app.core.network.dto.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request to update session settings.
 * PATCH v1/users/me/preferences/session
 */
@Serializable
data class UpdateSessionSettingsRequest(
    @SerialName("preferredSessionDuration") val preferredSessionDuration: Int,
    @SerialName("bufferBetweenSessions") val bufferBetweenSessions: Int
)
