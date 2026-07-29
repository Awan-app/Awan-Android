package com.awan.app.core.network.dto

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

@Serializable
data class UserPreferencesResponse(
    @SerialName("timezone") val timezone: String? = null,
    @SerialName("preferredSessionDuration") val preferredSessionDuration: Int? = null,
    @SerialName("bufferBetweenSessions") val bufferBetweenSessions: Int? = null,
    @SerialName("wakeupTime") val wakeupTime: String? = null,
    @SerialName("sleepTime") val sleepTime: String? = null,
    @SerialName("schedulingType") val schedulingType: String? = null,
)

