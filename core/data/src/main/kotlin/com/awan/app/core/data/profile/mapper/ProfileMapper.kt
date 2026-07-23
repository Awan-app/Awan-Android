package com.awan.app.core.data.profile.mapper

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
