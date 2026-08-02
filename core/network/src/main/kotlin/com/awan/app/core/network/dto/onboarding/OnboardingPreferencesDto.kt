package com.awan.app.core.network.dto.onboarding

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
