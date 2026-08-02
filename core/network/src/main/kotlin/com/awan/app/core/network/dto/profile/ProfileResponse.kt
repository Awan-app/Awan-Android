package com.awan.app.core.network.dto.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileResponse(
    @SerialName("id") val id: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("firstName") val firstName: String? = null,
    @SerialName("lastName") val lastName: String? = null,
    @SerialName("birthDate") val birthDate: String? = null,
    @SerialName("points") val points: Int? = null,
    @SerialName("streak") val streak: Int? = null,
    @SerialName("maxStreak") val maxStreak: Int? = null,
    @SerialName("preferences") val preferences: UserPreferencesResponse? = null,
)
