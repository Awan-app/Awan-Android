package com.awan.app.core.data.profile.repository

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.mapper.toDomain
import com.awan.app.core.domain.profile.model.Profile
import com.awan.app.core.domain.profile.repository.ProfileRepository
import com.awan.app.core.network.api.ProfileApiService
import com.awan.app.core.network.dto.request.AwardPointsRequest
import com.awan.app.core.network.dto.request.DeductPointsRequest
import com.awan.app.core.network.dto.request.UpdateBirthDateRequest
import com.awan.app.core.network.dto.request.UpdateNameRequest
import com.awan.app.core.network.dto.request.UpdateProfilePartialRequest
import com.awan.app.core.network.dto.request.UpdateSchedulingTypeRequest
import com.awan.app.core.network.dto.request.UpdateSessionSettingsRequest
import com.awan.app.core.network.dto.request.UpdateSleepScheduleRequest
import com.awan.app.core.network.dto.request.UpdateTimezoneRequest
import com.awan.app.core.network.error.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ProfileRepositoryImpl @Inject constructor(
    private val apiService: ProfileApiService,
    private val json: Json,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : ProfileRepository {

    override suspend fun getProfile(): Result<Profile> =
        safeApiCall(ioDispatcher, json) {
            apiService.getProfileInfo().toDomain()
        }

    override suspend fun updateName(firstName: String, lastName: String): Result<Profile> =
        safeApiCall(ioDispatcher, json) {
            apiService.updateProfileName(UpdateNameRequest(firstName, lastName)).toDomain()
        }

    override suspend fun updateBirthDate(birthDate: String): Result<Profile> =
        safeApiCall(ioDispatcher, json) {
            apiService.updateProfileBirthDate(UpdateBirthDateRequest(birthDate)).toDomain()
        }

    override suspend fun updateProfilePartial(
        firstName: String?,
        lastName: String?,
        timezone: String?,
        preferredSessionDuration: Int?,
        bufferBetweenSessions: Int?,
        wakeupTime: String?,
        sleepTime: String?,
        schedulingType: String?
    ): Result<Profile> = safeApiCall(ioDispatcher, json) {
        apiService.updateProfilePartial(
            UpdateProfilePartialRequest(
                firstName = firstName,
                lastName = lastName,
                timezone = timezone,
                preferredSessionDuration = preferredSessionDuration,
                bufferBetweenSessions = bufferBetweenSessions,
                wakeupTime = wakeupTime,
                sleepTime = sleepTime,
                schedulingType = schedulingType
            )
        ).toDomain()
    }

    override suspend fun updateTimezone(timezone: String): Result<Profile> =
        safeApiCall(ioDispatcher, json) {
            apiService.updateTimezone(UpdateTimezoneRequest(timezone)).toDomain()
        }

    override suspend fun updateSessionSettings(
        preferredSessionDuration: Int,
        bufferBetweenSessions: Int
    ): Result<Profile> = safeApiCall(ioDispatcher, json) {
        apiService.updateSessionSettings(
            UpdateSessionSettingsRequest(preferredSessionDuration, bufferBetweenSessions)
        ).toDomain()
    }

    override suspend fun updateSleepSchedule(wakeupTime: String, sleepTime: String): Result<Profile> =
        safeApiCall(ioDispatcher, json) {
            apiService.updateSleepSchedule(UpdateSleepScheduleRequest(wakeupTime, sleepTime)).toDomain()
        }

    override suspend fun updateSchedulingType(schedulingType: String): Result<Profile> =
        safeApiCall(ioDispatcher, json) {
            apiService.updateSchedulingType(UpdateSchedulingTypeRequest(schedulingType)).toDomain()
        }

    override suspend fun incrementStreak(): Result<Profile> =
        safeApiCall(ioDispatcher, json) {
            apiService.incrementStreak().toDomain()
        }

    override suspend fun resetStreak(): Result<Profile> =
        safeApiCall(ioDispatcher, json) {
            apiService.resetStreak().toDomain()
        }

    override suspend fun awardPoints(points: Int): Result<Profile> =
        safeApiCall(ioDispatcher, json) {
            apiService.awardPoints(AwardPointsRequest(points)).toDomain()
        }

    override suspend fun deductPoints(points: Int): Result<Profile> =
        safeApiCall(ioDispatcher, json) {
            apiService.deductPoints(DeductPointsRequest(points)).toDomain()
        }
}
