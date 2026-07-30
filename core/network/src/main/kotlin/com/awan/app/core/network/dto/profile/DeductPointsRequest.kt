package com.awan.app.core.network.dto.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request to deduct points.
 * PATCH v1/users/me/points/deduct
 */
@Serializable
data class DeductPointsRequest(
    @SerialName("points") val points: Int
)
