package com.awan.app.core.domain.profile.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.profile.model.Profile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observeProfile(): Flow<Profile?>

    suspend fun getProfile(): Result<Profile>
    suspend fun updateName(firstName: String, lastName: String): Result<Profile>
    suspend fun updateBirthDate(birthDate: String): Result<Profile>

    suspend fun updateProfilePicture(imageBytes: ByteArray, mimeType: String): Result<Profile>

    suspend fun deleteProfilePicture(): Result<Profile>

    suspend fun updateProfilePartial(
        firstName: String? = null,
        lastName: String? = null,
        timezone: String? = null,
        preferredSessionDuration: Int? = null,
        bufferBetweenSessions: Int? = null,
        wakeupTime: String? = null,
        sleepTime: String? = null,
        schedulingType: String? = null
    ): Result<Profile>

    suspend fun updateTimezone(timezone: String): Result<Profile>

    suspend fun updateSessionSettings(preferredSessionDuration: Int, bufferBetweenSessions: Int): Result<Profile>

    suspend fun updateSleepSchedule(wakeupTime: String, sleepTime: String): Result<Profile>

    suspend fun updateSchedulingType(schedulingType: String): Result<Profile>

    suspend fun incrementStreak(): Result<Profile>

    suspend fun resetStreak(): Result<Profile>

    suspend fun awardPoints(points: Int): Result<Profile>

    suspend fun deductPoints(points: Int): Result<Profile>
}