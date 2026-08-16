package com.awan.app.core.domain.onboarding.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.onboarding.model.OnboardingData

/**
 * Handles completing the user onboarding flow by persisting preferences locally and syncing with the backend.
 */
interface OnboardingRepository {
    suspend fun completeOnboarding(data: OnboardingData): Result<Unit>

    suspend fun hasCompletedOnboarding(): Boolean
}
