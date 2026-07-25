package com.awan.app.core.network.dto.onboarding

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CompleteOnboardingRequest(
    @SerialName("firstName") val firstName: String,
    @SerialName("lastName") val lastName: String,
    @SerialName("birthDate") val birthDate: String = "2000-01-01",
    @SerialName("timezone") val timezone: String = "Africa/Cairo",
    @SerialName("preferredSessionDuration") val preferredSessionDuration: Int = 30,
    @SerialName("bufferBetweenSessions") val bufferBetweenSessions: Int = 10,
    @SerialName("wakeupTime") val wakeupTime: String = "07:30:00",
    @SerialName("sleepTime") val sleepTime: String = "23:00:00",
    @SerialName("schedulingType") val schedulingType: String = "BALANCED",
)
