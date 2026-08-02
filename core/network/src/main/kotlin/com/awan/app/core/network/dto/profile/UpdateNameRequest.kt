package com.awan.app.core.network.dto.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request to update user's name.
 * PATCH v1/users/me/profile/name
 */
@Serializable
data class UpdateNameRequest(
    @SerialName("firstName") val firstName: String,
    @SerialName("lastName") val lastName: String
)
