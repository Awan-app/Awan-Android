package com.awan.feature.onboarding.impl.presentation

import com.awan.app.core.common.result.Result
import com.awan.app.core.data.onboarding.OnboardingData
import com.awan.app.core.data.onboarding.OnboardingRepository

class FakeOnboardingRepository : OnboardingRepository {
    var lastCompletedData: OnboardingData? = null
    var isCompleted: Boolean = false

    override suspend fun completeOnboarding(data: OnboardingData): Result<Unit> {
        lastCompletedData = data
        isCompleted = true
        return Result.Success(Unit)
    }
}
