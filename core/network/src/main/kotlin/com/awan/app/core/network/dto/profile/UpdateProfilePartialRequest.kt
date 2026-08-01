package com.awan.app.core.network.dto.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request for partial profile update.
 * PATCH v1/users/me
 */
@Serializable
data class UpdateProfilePartialRequest(
    @SerialName("firstName") val firstName: String? = null,
    @SerialName("lastName") val lastName: String? = null,
    @SerialName("timezone") val timezone: String? = null,
    @SerialName("preferredSessionDuration") val preferredSessionDuration: Int? = null,
    @SerialName("bufferBetweenSessions") val bufferBetweenSessions: Int? = null,
    @SerialName("wakeupTime") val wakeupTime: String? = null,
    @SerialName("sleepTime") val sleepTime: String? = null,
    @SerialName("schedulingType") val schedulingType: String? = null
)
