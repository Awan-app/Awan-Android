package com.awan.feature.onboarding.impl.presentation

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.onboarding.OnboardingData
import com.awan.app.core.data.onboarding.OnboardingRepository

class FakeOnboardingRepository : OnboardingRepository {
    var lastCompletedData: OnboardingData? = null
    var isCompleted: Boolean = false
    var callCount: Int = 0
    var failWith: AppError? = null

    override suspend fun completeOnboarding(data: OnboardingData): Result<Unit> {
        lastCompletedData = data
        isCompleted = true
        callCount++
        return failWith?.let { Result.Error(it) } ?: Result.Success(Unit)
    }
}
