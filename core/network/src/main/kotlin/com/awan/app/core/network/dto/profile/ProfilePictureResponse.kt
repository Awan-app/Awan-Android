package com.awan.app.core.network.dto.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfilePictureResponse(
    @SerialName("profilePictureUrl") val profilePictureUrl: String? = null
)
