package com.awan.app.core.network.api

import com.awan.app.core.network.dto.CompleteOnboardingRequest
import com.awan.app.core.network.dto.CompleteOnboardingResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface OnboardingApiService {

    @POST("v1/onboarding")
    suspend fun completeOnboarding(
        @Body request: CompleteOnboardingRequest,
    ): CompleteOnboardingResponse
}
