package com.awan.app.core.network.api

import com.awan.app.core.network.dto.CompleteOnboardingResponse
import retrofit2.http.GET

interface UserApiService {

    @GET("v1/users/me")
    suspend fun getUserProfile(): CompleteOnboardingResponse
}
