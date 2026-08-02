package com.awan.app.core.network.dto.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request to update user's timezone.
 * PATCH v1/users/me/preferences/timezone
 */
@Serializable
data class UpdateTimezoneRequest(
    @SerialName("timezone") val timezone: String
)
