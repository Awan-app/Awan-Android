package com.awan.app.core.network.dto.profile

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileResponse(
    @SerialName("id") val id: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("firstName") val firstName: String,
    @SerialName("lastName") val lastName: String,
    @SerialName("birthDate") val birthDate: String? = null,
    @SerialName("points") val points: Int = 0,
    @SerialName("streak") val streak: Int = 0,
    @SerialName("maxStreak") val maxStreak: Int = 0,
    @SerialName("preferences") val preferences: UserPreferencesResponse? = null,
)
