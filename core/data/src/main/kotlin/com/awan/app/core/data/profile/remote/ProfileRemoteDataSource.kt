package com.awan.app.core.data.profile.remote

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
import com.awan.app.core.common.result.Result


interface ProfileRemoteDataSource {

    suspend fun getProfileInfo(): Result<ProfileResponse>

    suspend fun updateProfileName(
        request: UpdateNameRequest,
    ): Result<ProfileResponse>

    suspend fun updateProfileBirthDate(
        request: UpdateBirthDateRequest,
    ): Result<ProfileResponse>

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