package com.awan.app.core.data.onboarding.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.CompleteOnboardingRequest
import com.awan.app.core.network.dto.CompleteOnboardingResponse

interface OnboardingRemoteDataSource {
    suspend fun completeOnboarding(request: CompleteOnboardingRequest): Result<CompleteOnboardingResponse>
}
