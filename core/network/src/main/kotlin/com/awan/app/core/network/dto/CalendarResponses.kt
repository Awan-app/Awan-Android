package com.awan.app.core.network.dto

import com.awan.app.core.network.dto.profile.UserPreferencesResponse
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
    @SerialName("profilePictureUrl") val profilePictureUrl: String? = null,
    @SerialName("isNew") val isNew: Boolean = true,
    @SerialName("preferences") val preferences: UserPreferencesResponse? = null,
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
