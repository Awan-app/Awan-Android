package com.awan.app.core.network.api

import com.awan.app.core.network.dto.IsNewResponse
import com.awan.app.core.network.dto.onboarding.CompleteOnboardingRequest
import com.awan.app.core.network.dto.onboarding.CompleteOnboardingResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface OnboardingApiService {

    @POST("v1/onboarding")
    suspend fun completeOnboarding(
        @Body request: CompleteOnboardingRequest,
    ): CompleteOnboardingResponse

    @GET("v1/users/me/is-new")
    suspend fun isNewUser(): IsNewResponse
}
