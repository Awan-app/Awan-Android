package com.awan.app.core.domain.profile.model

data class Profile(
    val id: String?,
    val email: String?,
    val firstName: String?,
    val lastName: String?,
    val birthDate: String?,
    val points: Int?,
    val streak: Int?,
    val maxStreak: Int?,
    val profilePictureUrl: String?,
    val isNew: Boolean?,
    val preferences: UserPreferences?
)

data class UserPreferences(
    val timezone: String?,
    val preferredSessionDuration: Int?,
    val bufferBetweenSessions: Int?,
    val wakeupTime: String?,
    val sleepTime: String?,
    val schedulingType: String?
)