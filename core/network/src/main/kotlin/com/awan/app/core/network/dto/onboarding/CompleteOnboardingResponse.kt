package com.awan.app.core.network.dto.onboarding

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CompleteOnboardingResponse(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String? = null,
    @SerialName("firstName") val firstName: String? = null,
    @SerialName("lastName") val lastName: String? = null,
    @SerialName("birthDate") val birthDate: String? = null,
    @SerialName("points") val points: Int? = 0,
    @SerialName("streak") val streak: Int? = 0,
    @SerialName("maxStreak") val maxStreak: Int? = 0,
    @SerialName("profilePictureUrl") val profilePictureUrl: String? = null,
    @SerialName("isNew") val isNew: Boolean? = null,
    @SerialName("preferences") val preferences: OnboardingPreferencesDto? = null,
)
