package com.awan.app.core.network.dto.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request to update scheduling type.
 * PATCH v1/users/me/preferences/scheduling-type
 */
@Serializable
data class UpdateSchedulingTypeRequest(
    @SerialName("schedulingType") val schedulingType: String // e.g. "BALANCED"
)
