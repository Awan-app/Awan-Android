package com.awan.app.core.network.dto.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request to award points.
 * PATCH v1/users/me/points/award
 */
@Serializable
data class AwardPointsRequest(
    @SerialName("points") val points: Int
)
