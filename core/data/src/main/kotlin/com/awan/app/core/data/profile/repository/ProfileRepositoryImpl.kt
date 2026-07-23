package com.awan.app.core.data.profile.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.common.result.map
import com.awan.app.core.common.result.onSuccess
import com.awan.app.core.data.profile.mapper.toDomain
import com.awan.app.core.data.profile.remote.ProfileRemoteDataSource
import com.awan.app.core.domain.profile.model.Profile
import com.awan.app.core.domain.profile.repository.ProfileRepository
import com.awan.app.core.network.dto.AwardPointsRequest
import com.awan.app.core.network.dto.DeductPointsRequest
import com.awan.app.core.network.dto.UpdateBirthDateRequest
import com.awan.app.core.network.dto.UpdateNameRequest
import com.awan.app.core.network.dto.UpdateProfilePartialRequest
import com.awan.app.core.network.dto.UpdateSchedulingTypeRequest
import com.awan.app.core.network.dto.UpdateSessionSettingsRequest
import com.awan.app.core.network.dto.UpdateSleepScheduleRequest
import com.awan.app.core.network.dto.UpdateTimezoneRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val profileRemoteDataSource: ProfileRemoteDataSource,
) : ProfileRepository {

    private val _profile = MutableStateFlow<Profile?>(null)

    override fun observeProfile(): Flow<Profile?> = _profile.asStateFlow()

    private fun updateCache(profile: Profile) {
        _profile.value = profile
    }

    override suspend fun getProfile(): Result<Profile> =
        profileRemoteDataSource.getProfileInfo()
            .map { it.toDomain() }
            .onSuccess(::updateCache)

    override suspend fun updateName(
        firstName: String,
        lastName: String,
    ): Result<Profile> =
        profileRemoteDataSource.updateProfileName(
            UpdateNameRequest(firstName = firstName, lastName = lastName),
        ).map { it.toDomain() }.onSuccess(::updateCache)

    override suspend fun updateBirthDate(birthDate: String): Result<Profile> =
        profileRemoteDataSource.updateProfileBirthDate(
            UpdateBirthDateRequest(birthDate = birthDate),
        ).map { it.toDomain() }.onSuccess(::updateCache)

    override suspend fun updateProfilePartial(
        firstName: String?,
        lastName: String?,
        timezone: String?,
        preferredSessionDuration: Int?,
        bufferBetweenSessions: Int?,
        wakeupTime: String?,
        sleepTime: String?,
        schedulingType: String?,
    ): Result<Profile> =
        profileRemoteDataSource.updateProfilePartial(
            UpdateProfilePartialRequest(
                firstName = firstName,
                lastName = lastName,
                timezone = timezone,
                preferredSessionDuration = preferredSessionDuration,
                bufferBetweenSessions = bufferBetweenSessions,
                wakeupTime = wakeupTime,
                sleepTime = sleepTime,
                schedulingType = schedulingType,
            ),
        ).map { it.toDomain() }.onSuccess(::updateCache)

    override suspend fun updateTimezone(timezone: String): Result<Profile> =
        profileRemoteDataSource.updateTimezone(
            UpdateTimezoneRequest(timezone = timezone),
        ).map { it.toDomain() }.onSuccess(::updateCache)

    override suspend fun updateSessionSettings(
        preferredSessionDuration: Int,
        bufferBetweenSessions: Int,
    ): Result<Profile> =
        profileRemoteDataSource.updateSessionSettings(
            UpdateSessionSettingsRequest(
                preferredSessionDuration = preferredSessionDuration,
                bufferBetweenSessions = bufferBetweenSessions,
            ),
        ).map { it.toDomain() }.onSuccess(::updateCache)

    override suspend fun updateSleepSchedule(
        wakeupTime: String,
        sleepTime: String,
    ): Result<Profile> =
        profileRemoteDataSource.updateSleepSchedule(
            UpdateSleepScheduleRequest(
                wakeupTime = wakeupTime,
                sleepTime = sleepTime,
            ),
        ).map { it.toDomain() }.onSuccess(::updateCache)

    override suspend fun updateSchedulingType(schedulingType: String): Result<Profile> =
        profileRemoteDataSource.updateSchedulingType(
            UpdateSchedulingTypeRequest(schedulingType = schedulingType),
        ).map { it.toDomain() }.onSuccess(::updateCache)

    override suspend fun incrementStreak(): Result<Profile> =
        profileRemoteDataSource.incrementStreak()
            .map { it.toDomain() }
            .onSuccess(::updateCache)

    override suspend fun resetStreak(): Result<Profile> =
        profileRemoteDataSource.resetStreak()
            .map { it.toDomain() }
            .onSuccess(::updateCache)

    override suspend fun awardPoints(points: Int): Result<Profile> =
        profileRemoteDataSource.awardPoints(
            AwardPointsRequest(points = points),
        ).map { it.toDomain() }.onSuccess(::updateCache)

    override suspend fun deductPoints(points: Int): Result<Profile> =
        profileRemoteDataSource.deductPoints(
            DeductPointsRequest(points = points),
        ).map { it.toDomain() }.onSuccess(::updateCache)
}
