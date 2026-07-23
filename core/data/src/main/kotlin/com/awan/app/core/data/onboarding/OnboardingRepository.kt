package com.awan.app.core.data.onboarding

import com.awan.app.core.common.result.Result

/**
 * Handles completing the user onboarding flow by persisting preferences locally and syncing with the backend.
 */
interface OnboardingRepository {
    suspend fun completeOnboarding(data: OnboardingData): Result<Unit>
}
