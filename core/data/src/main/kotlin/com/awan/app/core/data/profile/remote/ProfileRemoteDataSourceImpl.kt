package com.awan.app.core.data.profile.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.ProfileApiService
import com.awan.app.core.network.dto.AwardPointsRequest
import com.awan.app.core.network.dto.DeductPointsRequest
import com.awan.app.core.network.dto.ProfileResponse
import com.awan.app.core.network.dto.UpdateBirthDateRequest
import com.awan.app.core.network.dto.UpdateNameRequest
import com.awan.app.core.network.dto.UpdateProfilePartialRequest
import com.awan.app.core.network.dto.UpdateSchedulingTypeRequest
import com.awan.app.core.network.dto.UpdateSessionSettingsRequest
import com.awan.app.core.network.dto.UpdateSleepScheduleRequest
import com.awan.app.core.network.dto.UpdateTimezoneRequest
import com.awan.app.core.network.error.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ProfileRemoteDataSourceImpl @Inject constructor(
    private val profileApiService: ProfileApiService,
    private val json: Json,
    @Dispatcher(AwanDispatchers.IO)
    private val ioDispatcher: CoroutineDispatcher,
) : ProfileRemoteDataSource {

    override suspend fun getProfileInfo(): Result<ProfileResponse> =
        safeApiCall(
            dispatcher = ioDispatcher,
            json = json,
        ) {
            profileApiService.getProfileInfo()
        }

    override suspend fun updateProfileName(
        request: UpdateNameRequest,
    ): Result<ProfileResponse> =
        safeApiCall(
            dispatcher = ioDispatcher,
            json = json,
        ) {
            profileApiService.updateProfileName(request)
        }

    override suspend fun updateProfileBirthDate(
        request: UpdateBirthDateRequest,
    ): Result<ProfileResponse> =
        safeApiCall(
            dispatcher = ioDispatcher,
            json = json,
        ) {
            profileApiService.updateProfileBirthDate(request)
        }

    override suspend fun updateProfilePartial(
        request: UpdateProfilePartialRequest,
    ): Result<ProfileResponse> =
        safeApiCall(
            dispatcher = ioDispatcher,
            json = json,
        ) {
            profileApiService.updateProfilePartial(request)
        }

    override suspend fun updateTimezone(
        request: UpdateTimezoneRequest,
    ): Result<ProfileResponse> =
        safeApiCall(
            dispatcher = ioDispatcher,
            json = json,
        ) {
            profileApiService.updateTimezone(request)
        }

    override suspend fun updateSessionSettings(
        request: UpdateSessionSettingsRequest,
    ): Result<ProfileResponse> =
        safeApiCall(
            dispatcher = ioDispatcher,
            json = json,
        ) {
            profileApiService.updateSessionSettings(request)
        }

    override suspend fun updateSleepSchedule(
        request: UpdateSleepScheduleRequest,
    ): Result<ProfileResponse> =
        safeApiCall(
            dispatcher = ioDispatcher,
            json = json,
        ) {
            profileApiService.updateSleepSchedule(request)
        }

    override suspend fun updateSchedulingType(
        request: UpdateSchedulingTypeRequest,
    ): Result<ProfileResponse> =
        safeApiCall(
            dispatcher = ioDispatcher,
            json = json,
        ) {
            profileApiService.updateSchedulingType(request)
        }

    override suspend fun incrementStreak(): Result<ProfileResponse> =
        safeApiCall(
            dispatcher = ioDispatcher,
            json = json,
        ) {
            profileApiService.incrementStreak()
        }

    override suspend fun resetStreak(): Result<ProfileResponse> =
        safeApiCall(
            dispatcher = ioDispatcher,
            json = json,
        ) {
            profileApiService.resetStreak()
        }

    override suspend fun awardPoints(
        request: AwardPointsRequest,
    ): Result<ProfileResponse> =
        safeApiCall(
            dispatcher = ioDispatcher,
            json = json,
        ) {
            profileApiService.awardPoints(request)
        }

    override suspend fun deductPoints(
        request: DeductPointsRequest,
    ): Result<ProfileResponse> =
        safeApiCall(
            dispatcher = ioDispatcher,
            json = json,
        ) {
            profileApiService.deductPoints(request)
        }
}