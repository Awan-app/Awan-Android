package com.awan.app.core.data.profile.mapper

import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.database.model.UserPreferencesEntity
import com.awan.app.core.database.model.UserWithPreferences
import com.awan.app.core.domain.profile.model.Profile
import com.awan.app.core.domain.profile.model.UserPreferences
import com.awan.app.core.network.dto.ProfileResponse
import com.awan.app.core.network.dto.UserPreferencesResponse

@Suppress("UNCHECKED_CAST")
internal fun ProfileResponse.toDomain(): Profile = Profile(
    id = id,
    email = email,
    firstName = firstName,
    lastName = lastName,
    birthDate = birthDate,
    points = points,
    streak = streak,
    maxStreak = maxStreak,
    preferences = preferences?.toDomain(),
)

internal fun UserPreferencesResponse.toDomain(): UserPreferences = UserPreferences(
    timezone = timezone,
    preferredSessionDuration = preferredSessionDuration,
    bufferBetweenSessions = bufferBetweenSessions,
    wakeupTime = wakeupTime,
    sleepTime = sleepTime,
    schedulingType = schedulingType,
)

internal fun UserWithPreferences.asExternalModel(): Profile = Profile(
    id = user.id,
    email = user.email,
    firstName = user.firstName,
    lastName = user.lastName,
    birthDate = user.birthDate,
    points = user.points,
    streak = user.streak,
    maxStreak = user.maxStreak,
    preferences = preferences?.asExternalModel(),
)

internal fun UserPreferencesEntity.asExternalModel(): UserPreferences = UserPreferences(
    timezone = timezone,
    preferredSessionDuration = preferredSessionDuration,
    bufferBetweenSessions = bufferBetweenSessions,
    wakeupTime = wakeupTime,
    sleepTime = sleepTime,
    schedulingType = schedulingType,
)

internal fun Profile.asEntity(): UserEntity = UserEntity(
    id = id ?: "",
    email = email ?: "",
    firstName = firstName ?: "",
    lastName = lastName ?: "",
    birthDate = birthDate,
    points = points ?: 0,
    streak = streak ?: 0,
    maxStreak = maxStreak ?: 0,
)

internal fun UserPreferences.asEntity(userId: String): UserPreferencesEntity = UserPreferencesEntity(
    userId = userId,
    timezone = timezone ?: "UTC",
    preferredSessionDuration = preferredSessionDuration ?: 60,
    bufferBetweenSessions = bufferBetweenSessions ?: 5,
    wakeupTime = wakeupTime ?: "07:00:00",
    sleepTime = sleepTime ?: "23:00:00",
    schedulingType = schedulingType ?: "BALANCED",
)
