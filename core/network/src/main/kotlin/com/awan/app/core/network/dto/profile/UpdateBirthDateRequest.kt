package com.awan.app.core.network.dto.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request to update user's birth date.
 * PATCH v1/users/me/profile/birth-date
 */
@Serializable
data class UpdateBirthDateRequest(
    @SerialName("birthDate") val birthDate: String // "yyyy-MM-dd"
)
