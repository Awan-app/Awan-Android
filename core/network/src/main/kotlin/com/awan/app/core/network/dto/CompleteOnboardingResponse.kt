package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OnboardingPreferencesDto(
    @SerialName("timezone") val timezone: String? = null,
    @SerialName("preferredSessionDuration") val preferredSessionDuration: Int? = null,
    @SerialName("bufferBetweenSessions") val bufferBetweenSessions: Int? = null,
    @SerialName("wakeupTime") val wakeupTime: String? = null,
    @SerialName("sleepTime") val sleepTime: String? = null,
    @SerialName("schedulingType") val schedulingType: String? = null,
)

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
    @SerialName("preferences") val preferences: OnboardingPreferencesDto? = null,
)
