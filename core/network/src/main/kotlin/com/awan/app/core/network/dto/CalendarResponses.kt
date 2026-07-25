package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponse(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String = "",
    @SerialName("firstName") val firstName: String? = null,
    @SerialName("lastName") val lastName: String? = null,
    @SerialName("birthDate") val birthDate: String? = null,
    @SerialName("points") val points: Int = 0,
    @SerialName("streak") val streak: Int = 0,
    @SerialName("maxStreak") val maxStreak: Int = 0,
    @SerialName("preferences") val preferences: UserPreferencesResponse? = null,
)

@Serializable
data class UserPreferencesResponse(
    @SerialName("timezone") val timezone: String? = null,
    @SerialName("preferredSessionDuration") val preferredSessionDuration: Int = 0,
    @SerialName("bufferBetweenSessions") val bufferBetweenSessions: Int = 0,
    @SerialName("wakeupTime") val wakeupTime: String = "",
    @SerialName("sleepTime") val sleepTime: String = "",
    @SerialName("schedulingType") val schedulingType: String = "BALANCED",
)

@Serializable
data class GoalResponse(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("status") val status: String = "ACTIVE",
    @SerialName("targetDate") val targetDate: String? = null,
    @SerialName("createdAt") val createdAt: String = "",
    @SerialName("inbox") val inbox: Boolean = false,
)

@Serializable
data class GoalPageResponse(
    @SerialName("content") val content: List<GoalResponse> = emptyList(),
    @SerialName("totalPages") val totalPages: Int = 1,
    @SerialName("last") val last: Boolean = true,
)
