package com.awan.feature.onboarding.impl.presentation

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.onboarding.model.OnboardingData
import com.awan.app.core.domain.onboarding.repository.OnboardingRepository

class FakeOnboardingRepository : OnboardingRepository {
    var lastCompletedData: OnboardingData? = null
    var isCompleted: Boolean = false
    var hasCompleted: Boolean = false
    var callCount: Int = 0
    var failWith: AppError? = null

    override suspend fun completeOnboarding(data: OnboardingData): Result<Unit> {
        lastCompletedData = data
        callCount++
        return failWith?.let { Result.Error(it) } ?: run {
            isCompleted = true
            Result.Success(Unit)
        }
    }

    override suspend fun hasCompletedOnboarding(): Boolean = hasCompleted
}
