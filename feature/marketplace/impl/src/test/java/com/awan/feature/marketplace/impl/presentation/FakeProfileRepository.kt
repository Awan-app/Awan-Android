package com.awan.feature.marketplace.impl.presentation

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.profile.model.Profile
import com.awan.app.core.domain.profile.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeProfileRepository : ProfileRepository {
    val profileFlow = MutableStateFlow<Profile?>(null)

    override fun observeProfile(): Flow<Profile?> = profileFlow

    override suspend fun getProfile(): Result<Profile> {
        return profileFlow.value?.let { Result.Success(it) } ?: Result.Error(com.awan.app.core.common.error.AppError.NotFound)
    }

    override suspend fun updateName(firstName: String, lastName: String): Result<Profile> = TODO()
    override suspend fun updateBirthDate(birthDate: String): Result<Profile> = TODO()
    override suspend fun updateProfilePicture(imageBytes: ByteArray, mimeType: String): Result<Profile> = TODO()
    override suspend fun deleteProfilePicture(): Result<Profile> = TODO()
    override suspend fun updateProfilePartial(firstName: String?, lastName: String?, timezone: String?, preferredSessionDuration: Int?, bufferBetweenSessions: Int?, wakeupTime: String?, sleepTime: String?, schedulingType: String?): Result<Profile> = TODO()
    override suspend fun updateTimezone(timezone: String): Result<Profile> = TODO()
    override suspend fun updateSessionSettings(preferredSessionDuration: Int, bufferBetweenSessions: Int): Result<Profile> = TODO()
    override suspend fun updateSleepSchedule(wakeupTime: String, sleepTime: String): Result<Profile> = TODO()
    override suspend fun updateSchedulingType(schedulingType: String): Result<Profile> = TODO()
    override suspend fun incrementStreak(): Result<Profile> = TODO()
    override suspend fun resetStreak(): Result<Profile> = TODO()
    override suspend fun awardPoints(points: Int): Result<Profile> = TODO()
    override suspend fun deductPoints(points: Int): Result<Profile> = TODO()
}
