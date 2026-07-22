package com.awan.app.core.network.api

import com.awan.app.core.network.dto.ProfileResponse
import com.awan.app.core.network.dto.request.AwardPointsRequest
import com.awan.app.core.network.dto.request.DeductPointsRequest
import com.awan.app.core.network.dto.request.UpdateBirthDateRequest
import com.awan.app.core.network.dto.request.UpdateNameRequest
import com.awan.app.core.network.dto.request.UpdateProfilePartialRequest
import com.awan.app.core.network.dto.request.UpdateSchedulingTypeRequest
import com.awan.app.core.network.dto.request.UpdateSessionSettingsRequest
import com.awan.app.core.network.dto.request.UpdateSleepScheduleRequest
import com.awan.app.core.network.dto.request.UpdateTimezoneRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface ProfileApiService {

    @GET("v1/users/me")
    suspend fun getProfileInfo(): ProfileResponse

    @PATCH("v1/users/me/profile/name")
    suspend fun updateProfileName(@Body request: UpdateNameRequest): ProfileResponse

    @PATCH("v1/users/me/profile/birth-date")
    suspend fun updateProfileBirthDate(@Body request: UpdateBirthDateRequest): ProfileResponse

    @PATCH("v1/users/me")
    suspend fun updateProfilePartial(@Body request: UpdateProfilePartialRequest): ProfileResponse

    @PATCH("v1/users/me/preferences/timezone")
    suspend fun updateTimezone(@Body request: UpdateTimezoneRequest): ProfileResponse

    @PATCH("v1/users/me/preferences/session")
    suspend fun updateSessionSettings(@Body request: UpdateSessionSettingsRequest): ProfileResponse

    @PATCH("v1/users/me/preferences/sleep-schedule")
    suspend fun updateSleepSchedule(@Body request: UpdateSleepScheduleRequest): ProfileResponse

    @PATCH("v1/users/me/preferences/scheduling-type")
    suspend fun updateSchedulingType(@Body request: UpdateSchedulingTypeRequest): ProfileResponse

    @PATCH("v1/users/me/streak/increment")
    suspend fun incrementStreak(): ProfileResponse

    @PATCH("v1/users/me/streak/reset")
    suspend fun resetStreak(): ProfileResponse

    @PATCH("v1/users/me/points/award")
    suspend fun awardPoints(@Body request: AwardPointsRequest): ProfileResponse

    @PATCH("v1/users/me/points/deduct")
    suspend fun deductPoints(@Body request: DeductPointsRequest): ProfileResponse
}
