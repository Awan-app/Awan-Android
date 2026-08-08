package com.awan.app.core.data.profile.remote

import com.awan.app.core.network.dto.profile.AwardPointsRequest
import com.awan.app.core.network.dto.profile.DeductPointsRequest
import com.awan.app.core.network.dto.profile.ProfilePictureResponse
import com.awan.app.core.network.dto.profile.ProfileResponse
import com.awan.app.core.network.dto.profile.UpdateBirthDateRequest
import com.awan.app.core.network.dto.profile.UpdateNameRequest
import com.awan.app.core.network.dto.profile.UpdateProfilePartialRequest
import com.awan.app.core.network.dto.profile.UpdateSchedulingTypeRequest
import com.awan.app.core.network.dto.profile.UpdateSessionSettingsRequest
import com.awan.app.core.network.dto.profile.UpdateSleepScheduleRequest
import com.awan.app.core.network.dto.profile.UpdateTimezoneRequest
import com.awan.app.core.common.result.Result


interface ProfileRemoteDataSource {

    suspend fun getProfileInfo(): Result<ProfileResponse>

    suspend fun updateProfileName(
        request: UpdateNameRequest,
    ): Result<ProfileResponse>

    suspend fun updateProfileBirthDate(
        request: UpdateBirthDateRequest,
    ): Result<ProfileResponse>

    suspend fun updateProfilePicture(
        imageBytes: ByteArray,
        mimeType: String,
    ): Result<ProfilePictureResponse>

    suspend fun deleteProfilePicture(): Result<Unit>

    suspend fun updateProfilePartial(
        request: UpdateProfilePartialRequest,
    ): Result<ProfileResponse>

    suspend fun updateTimezone(
        request: UpdateTimezoneRequest,
    ): Result<ProfileResponse>

    suspend fun updateSessionSettings(
        request: UpdateSessionSettingsRequest,
    ): Result<ProfileResponse>

    suspend fun updateSleepSchedule(
        request: UpdateSleepScheduleRequest,
    ): Result<ProfileResponse>

    suspend fun updateSchedulingType(
        request: UpdateSchedulingTypeRequest,
    ): Result<ProfileResponse>

    suspend fun incrementStreak(): Result<ProfileResponse>

    suspend fun resetStreak(): Result<ProfileResponse>

    suspend fun awardPoints(
        request: AwardPointsRequest,
    ): Result<ProfileResponse>

    suspend fun deductPoints(
        request: DeductPointsRequest,
    ): Result<ProfileResponse>
}