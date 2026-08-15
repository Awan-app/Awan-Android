package com.awan.app.core.data.profile.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.ProfileApiService
import com.awan.app.core.network.dto.profile.ProfilePictureResponse
import com.awan.app.core.network.dto.profile.ProfileResponse
import com.awan.app.core.network.dto.profile.UpdateBirthDateRequest
import com.awan.app.core.network.dto.profile.UpdateNameRequest
import com.awan.app.core.network.dto.profile.UpdateProfilePartialRequest
import com.awan.app.core.network.dto.profile.UpdateSchedulingTypeRequest
import com.awan.app.core.network.dto.profile.UpdateSessionSettingsRequest
import com.awan.app.core.network.dto.profile.UpdateSleepScheduleRequest
import com.awan.app.core.network.dto.profile.UpdateTimezoneRequest
import com.awan.app.core.network.error.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
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

    override suspend fun updateProfilePicture(
        imageBytes: ByteArray,
        mimeType: String,
    ): Result<ProfilePictureResponse> =
        safeApiCall(
            dispatcher = ioDispatcher,
            json = json,
        ) {
            val extension = when (mimeType.lowercase()) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                else -> "jpg"
            }
            val requestFile = imageBytes.toRequestBody(mimeType.toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("image", "profile_picture.$extension", requestFile)
            profileApiService.updateProfilePicture(body)
        }

    override suspend fun deleteProfilePicture(): Result<Unit> =
        safeApiCall(
            dispatcher = ioDispatcher,
            json = json,
        ) {
            profileApiService.deleteProfilePicture()
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
}