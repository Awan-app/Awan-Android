package com.awan.app.core.network.api

import com.awan.app.core.network.dto.onboarding.CompleteOnboardingResponse
import retrofit2.http.GET

interface UserApiService {

    @GET("v1/users/me")
    suspend fun getUserProfile(): CompleteOnboardingResponse
}
